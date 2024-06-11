package net.tomasbot.matchday.api.service.admin;

import net.tomasbot.matchday.model.ApplicationInfo;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfoService {

  private static final String osData = getOsData();
  private static final Long pid = Long.parseLong(System.getProperty("PID"));
  private final BuildProperties buildProperties;

  ApplicationInfoService(BuildProperties properties) {
    this.buildProperties = properties;
  }

  private static String getOsData() {
    final String name = System.getProperty("os.name");
    final String version = System.getProperty("os.version");
    final String arch = System.getProperty("os.arch");
    return String.format("%s %s %s", name, version, arch);
  }

  public ApplicationInfo getApplicationInfo() {
    final String appVersion = buildProperties.getVersion();
    return ApplicationInfo.builder().version(appVersion).system(osData).pid(pid).build();
  }
}
