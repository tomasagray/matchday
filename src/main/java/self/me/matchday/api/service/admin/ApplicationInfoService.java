package self.me.matchday.api.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfoService {

  private static final String osData = getOsData();
  private static final Long pid = Long.parseLong(System.getProperty("PID"));
  private final BuildProperties buildProperties;

  @Value("${system.info.external-ip-service-url}")
  private URL ipServiceUrl;

  ApplicationInfoService(BuildProperties properties) {
    this.buildProperties = properties;
  }

  public ApplicationInfo getApplicationInfo() {
    final String appVersion = buildProperties.getVersion();
    return ApplicationInfo.builder()
        .version(appVersion)
        .system(osData)
        .pid(pid)
        .ip(getIpAddress())
        .build();
  }

  private static String getOsData() {
    final String name = System.getProperty("os.name");
    final String version = System.getProperty("os.version");
    final String arch = System.getProperty("os.arch");
    return String.format("%s %s %s", name, version, arch);
  }

  private @NotNull String getIpAddress() {

    UrlResource resource = new UrlResource(ipServiceUrl);
    try (InputStreamReader in =
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(in)) {

      String data = reader.lines().collect(Collectors.joining("\n"));
      Document document = Jsoup.parse(data);
      Elements input = document.select("input#ip");
      return input.attr("value");

    } catch (IOException e) {
      return "Error: " + e.getMessage();
    }
  }

  @Data
  @Builder
  public static class ApplicationInfo {
    private String version;
    private String system;
    private Long pid;
    private String ip;
  }
}
