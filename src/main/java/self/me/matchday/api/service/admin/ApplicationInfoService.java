package self.me.matchday.api.service.admin;

import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import self.me.matchday.model.ApplicationInfo;

@Service
public class ApplicationInfoService {

  private static final String osData = getOsData();
  private static final Long pid = Long.parseLong(System.getProperty("PID"));
  private final BuildProperties buildProperties;

  ApplicationInfoService(BuildProperties properties) {
    this.buildProperties = properties;
  }

  public ApplicationInfo getApplicationInfo() {
    final String appVersion = buildProperties.getVersion();
    return ApplicationInfo.builder().version(appVersion).system(osData).pid(pid).build();
  }

  private static String getOsData() {
    final String name = System.getProperty("os.name");
    final String version = System.getProperty("os.version");
    final String arch = System.getProperty("os.arch");
    return String.format("%s %s %s", name, version, arch);
  }
}
