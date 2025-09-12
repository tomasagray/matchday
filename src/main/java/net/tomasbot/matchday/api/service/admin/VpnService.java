package net.tomasbot.matchday.api.service.admin;

import static net.tomasbot.matchday.api.controller.VpnStatusController.VPN_STATUS_EMIT_ENDPOINT;
import static net.tomasbot.matchday.config.settings.UnprotectedAddress.UNPROTECTED_ADDR;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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
  private static final String ERROR_IP = "☠️.☠️.☠️.☠️";
  private static final String CONNECTING_IP = "---.---.---.---";
  private static final String UNKNOWN_SERVER = "????";

  // signals
  private static final String SIGTERM = "SIGTERM"; // stop
  //  private static final String SIGUSR2 = "SIGUSR2"; // get status
  // status
  //  private static final String SUCCESS = "SUCCESS:"; // vpn connected

  private static final VpnStatus CONNECTING_STATUS =
      new VpnStatus(VpnConnectionStatus.CONNECTING, CONNECTING_IP);
  private static final Random R = new Random();

  private final TelnetClientWrapper telnet;
  private final IpService ipService;
  private final SimpMessagingTemplate messagingTemplate;
  private final VpnStatusController statusController;
  private final SettingsService settingsService;

  @Value("${system.vpn.startup-arguments}")
  private String arguments;

  @Value("${system.vpn.management.port}")
  private Integer managementPort;

  private Collection<Path> vpnConfigurations;
  private Path currentConfiguration;

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

  public Collection<Path> getConfigurations() throws IOException {
    if (vpnConfigurations == null) vpnConfigurations = readConfigurations();
    return vpnConfigurations;
  }

  public void publishVpnStatus(@NotNull VpnStatus status) {
    statusController.setVpnStatus(status);
    messagingTemplate.convertAndSend(VPN_STATUS_EMIT_ENDPOINT, statusController.publishVpnStatus());
  }

  private @NotNull List<String> getStartupArguments() {
    List<String> args =
        Arrays.stream(arguments.split(",")) // split args
            .map(arg -> "--" + arg) // add leading dashes where required
            .toList();
    return new ArrayList<>(args); // return a mutable copy
  }

  private @NotNull Path getRandomConfiguration() throws IOException {
    final List<Path> configurations = getConfigurations().stream().toList();

    final int configCount = configurations.size();
    if (configCount == 0)
      throw new FileNotFoundException("Did not find any VPN configurations");

    int random = R.nextInt(configCount);
    return configurations.get(random);
  }

  private @NotNull List<Path> readConfigurations() throws IOException {
    if (!Files.exists(CONFIG_LOCATION))
      throw new FileNotFoundException("Could not find VPN config location: " + CONFIG_LOCATION);

    final List<Path> configurations = new ArrayList<>();
    Files.walkFileTree(
            CONFIG_LOCATION,
            new SimpleFileVisitor<>() {
              @Override
              public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
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

  private String getVpnServer() {
    final String filename = this.currentConfiguration.getFileName().toString();
    final String[] parts = filename.split("\\.");
    return parts.length > 0 ? parts[0] : UNKNOWN_SERVER;
  }

  public void start() throws Throwable {
    publishVpnStatus(CONNECTING_STATUS);

    try {
      // prepare arguments
      final List<String> arguments = new ArrayList<>();
      arguments.add("openvpn");
      arguments.add("--config");
      this.currentConfiguration = getRandomConfiguration();
      arguments.add(this.currentConfiguration.toString());
      arguments.addAll(getStartupArguments());

      // assemble arguments
      String cmd = String.join(" ", arguments);

      // NOTE: using deprecated exec(String) method as exec(String[]) does
      // not seem to correctly parse the arguments
      // TODO: change this to work with exec(String[])
      final Process process = Runtime.getRuntime().exec(cmd);

      // allow VPN time to start...
      process.waitFor();
      waitForIpRecheck();
    } finally {
      heartbeat();
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

  public void restart() throws Throwable {
    try {
      stop();

      publishVpnStatus(CONNECTING_STATUS);
      // wait before reconnecting
      waitForIpRecheck();

      start();
    } finally {
      heartbeat();
    }
  }

  public void heartbeat() throws Throwable {
    String unprotectedIp = settingsService.getSetting(UNPROTECTED_ADDR, String.class);
    doHeartbeat(unprotectedIp);
  }

  private void doHeartbeat(String unprotectedIp) throws Throwable {
    if (unprotectedIp == null || unprotectedIp.isEmpty()) {
      handleAmbiguousProtection();
      return;
    }

    try {
      final String currentIpAddress = ipService.getIpAddress();

      if (!currentIpAddress.equals(unprotectedIp)) {
        final String server = getVpnServer();
        publishVpnStatus(new VpnStatus(VpnConnectionStatus.CONNECTED, currentIpAddress, server));
      } else {
        publishVpnStatus(new VpnStatus(VpnConnectionStatus.DISCONNECTED, currentIpAddress));
      }
    } catch (Throwable e) {
      handleConnectionError(e);
    }
  }

  private void handleConnectionError(@NotNull Throwable e) throws Throwable {
    publishVpnStatus(new VpnStatus(VpnConnectionStatus.ERROR, ERROR_IP));
    throw e;
  }

  private void handleAmbiguousProtection() {
    publishVpnStatus(new VpnStatus(VpnConnectionStatus.ERROR, ERROR_IP));
  }
}
