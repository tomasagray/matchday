package net.tomasbot.matchday.api.service.admin;

import java.io.IOException;
import net.tomasbot.matchday.model.ApplicationInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfoService {

  private static final String osData = getOsData();
  private static final Long pid = Long.parseLong(System.getProperty("PID"));

  private final IpService ipService;
  private final BuildProperties buildProperties;

  ApplicationInfoService(IpService ipService, BuildProperties properties) {
    this.ipService = ipService;
    this.buildProperties = properties;
  }

  private static @NotNull String getOsData() {
    String name = System.getProperty("os.name");
    String version = System.getProperty("os.version");
    String arch = System.getProperty("os.arch");

    return String.format("%s %s %s", name, version, arch);
  }

  public ApplicationInfo getApplicationInfo() throws IOException {
    String appVersion = buildProperties.getVersion();
    String ipAddress = ipService.getIpAddress();

    return ApplicationInfo.builder()
        .pid(pid)
        .system(osData)
        .appVersion(appVersion)
        .ipAddress(ipAddress)
        .build();
  }
}
