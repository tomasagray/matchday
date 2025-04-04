package net.tomasbot.matchday.api.service.admin;

import static net.tomasbot.matchday.api.controller.VpnStatusController.VPN_STATUS_EMIT_ENDPOINT;
import static net.tomasbot.matchday.config.settings.UnprotectedAddress.UNPROTECTED_ADDR;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.tomasbot.matchday.api.controller.VpnStatusController;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.model.VpnStatus;
import net.tomasbot.matchday.model.VpnStatus.VpnConnectionStatus;
import net.tomasbot.matchday.util.TelnetClientWrapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class VpnService {

  private static final int IP_RECHECK_WAIT = 2_000;

  private static final String HOST = "localhost";
  private static final String DEFAULT_VPN_PREFIX = "us";
  private static final String VPN_FILE_EXT = ".ovpn";
  private static final String TERMINATOR = "\n";
  private static final Path CONFIG_LOCATION = Path.of("/etc/openvpn/ovpn_udp");
  // signals
  private static final String SIGTERM = "SIGTERM"; // stop
  private static final String SIGUSR2 = "SIGUSR2"; // get status
  // status
  private static final String SUCCESS = "SUCCESS:"; // vpn connected
  private static final String CONNECTING_IP = "---.---.---.---";
  private static final VpnStatus CONNECTING_STATUS =
      new VpnStatus(VpnConnectionStatus.CONNECTING, CONNECTING_IP);

  private final TelnetClientWrapper telnet;
  private final IpService ipService;
  private final SimpMessagingTemplate messagingTemplate;
  private final VpnStatusController statusController;
  private final SettingsService settingsService;

  @Value("${system.vpn.startup-arguments}")
  private String arguments;

  @Value("${system.vpn.management.port}")
  private Integer managementPort;

  public VpnService(
      TelnetClientWrapper telnet,
      IpService ipService,
      SimpMessagingTemplate messagingTemplate,
      VpnStatusController statusController,
      SettingsService settingsService) {
    this.telnet = telnet;
    this.ipService = ipService;
    this.messagingTemplate = messagingTemplate;
    this.statusController = statusController;
    this.settingsService = settingsService;
  }

  private static void waitForIpRecheck() throws InterruptedException {
    TimeUnit.MILLISECONDS.sleep(IP_RECHECK_WAIT);
  }

  public void publishVpnStatus(@NotNull VpnStatus status) {
    statusController.setVpnStatus(status);
    messagingTemplate.convertAndSend(VPN_STATUS_EMIT_ENDPOINT, statusController.publishVpnStatus());
  }

  private @NotNull List<String> getArguments() {
    List<String> args = Arrays.stream(arguments.split(",")).map(arg -> "--" + arg).toList();
    return new ArrayList<>(args); // return mutable copy
  }

  private @NotNull Path getRandomConfiguration() throws IOException {
    final Random r = new Random();
    final List<Path> configurations = getConfigurations();
    int configCount = configurations.size();
    if (configCount == 0) {
      throw new FileNotFoundException("Did not find any VPN configurations");
    }
    int random = r.nextInt(configCount - 1);
    return configurations.get(random);
  }

  private @NotNull List<Path> getConfigurations() throws IOException {
    final List<Path> configurations = new ArrayList<>();
    Files.walkFileTree(
        CONFIG_LOCATION,
        new SimpleFileVisitor<>() {
          @Override
          public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) {
            final String fileName = file.getFileName().toString();
            if (fileName.startsWith(DEFAULT_VPN_PREFIX) && fileName.endsWith(VPN_FILE_EXT)) {
              configurations.add(file);
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return configurations;
  }

  public String signal(@NotNull String signal) throws IOException {
    telnet.send("signal " + signal);
    return telnet.receive(TERMINATOR);
  }

  private String getVpnServer(@NotNull Path configuration) {
    final String filename = configuration.getFileName().toString();
    final String[] parts = filename.split("\\.");
    if (parts.length > 0) {
      return parts[0];
    }
    return "????";
  }

  public void start() throws Exception {
    publishVpnStatus(CONNECTING_STATUS);

    final List<String> arguments = getArguments();
    final Path configuration = getRandomConfiguration();
    arguments.add(0, "--config " + configuration);
    final String cmd = "openvpn " + String.join(" ", arguments);

    final Process process = Runtime.getRuntime().exec(cmd);
    process.waitFor();
    waitForIpRecheck();

    final VpnConnectionStatus vpnStatus = getVpnStatus();
    if (vpnStatus.equals(VpnConnectionStatus.CONNECTED)) {
      String ipAddress = ipService.getIpAddress();
      String vpnServer = getVpnServer(configuration);
      publishVpnStatus(new VpnStatus(vpnStatus, ipAddress, vpnServer));
    } else {
      doHeartbeat();
    }
  }

  public void stop() throws IOException {
    try {
      telnet.connect(HOST, managementPort);
      signal(SIGTERM);
      telnet.disconnect();
      waitForIpRecheck();
    } catch (Exception ignore) {
      // VPN is already disconnected
    } finally {
      final String ipAddress = ipService.getIpAddress();
      final VpnStatus status = new VpnStatus(VpnConnectionStatus.DISCONNECTED, ipAddress);
      publishVpnStatus(status);
    }
  }

  public void restart() throws Exception {
    try {
      publishVpnStatus(CONNECTING_STATUS);
      telnet.connect(HOST, managementPort);
      signal(SIGTERM);
      telnet.disconnect();
      // wait before reconnecting
      waitForIpRecheck();
      start();
    } finally {
      doHeartbeat();
    }
  }

  private VpnConnectionStatus getVpnStatus() throws IOException {
    VpnConnectionStatus vpnConnectionStatus = VpnConnectionStatus.DISCONNECTED;
    try {
      telnet.connect(HOST, managementPort);
      telnet.receive(TERMINATOR);

      final String status = signal(SIGUSR2);
      if (status.startsWith(SUCCESS)) {
        vpnConnectionStatus = VpnConnectionStatus.CONNECTED;
      }

      telnet.disconnect();
    } catch (ConnectException ignore) {
      // VPN disconnected
    }

    return vpnConnectionStatus;
  }

  public void doHeartbeat() {
    String unprotectedIp = settingsService.getSetting(UNPROTECTED_ADDR, String.class);
    doHeartbeat(unprotectedIp);
  }

  private void doHeartbeat(String unprotectedIp) {
    if (unprotectedIp == null || unprotectedIp.isEmpty()) {
      publishVpnStatus(new VpnStatus(VpnConnectionStatus.ERROR, null));
      return;
    }

    try {
      String currentIpAddress = ipService.getIpAddress();
      if (!currentIpAddress.equals(unprotectedIp)) {
        publishVpnStatus(new VpnStatus(VpnConnectionStatus.CONNECTED, currentIpAddress));
      } else {
        publishVpnStatus(new VpnStatus(VpnConnectionStatus.DISCONNECTED, currentIpAddress));
      }
    } catch (IOException e) {
      publishVpnStatus(new VpnStatus(VpnConnectionStatus.ERROR, null));
    }
  }
}
