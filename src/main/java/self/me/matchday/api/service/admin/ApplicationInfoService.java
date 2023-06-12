package self.me.matchday.api.service.admin;

import lombok.Builder;
import lombok.Data;
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

  @Data
  @Builder
  public static class ApplicationInfo {
    private String version;
    private String system;
    private Long pid;
  }
}
