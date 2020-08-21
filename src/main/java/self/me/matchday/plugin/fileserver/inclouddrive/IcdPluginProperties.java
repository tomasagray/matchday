package self.me.matchday.plugin.fileserver.inclouddrive;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

@EqualsAndHashCode(callSuper = true)
@Data
@Configuration
@PropertySource("classpath:plugins/icd/icd.properties")
@ConfigurationProperties(prefix = "plugin.icd")
public class IcdPluginProperties extends PluginProperties {

  private String urlPattern;
  private String linkIdentifier;
  private String userDataIdentifier;
  @Value("${plugin.icd.user-agent.mac}")    // mac & win versions in properties file
  private String userAgent;
  private ICDUrl url;
  private int defaultRefreshRate;

  public String getBaseUrl() {
    return url.getBaseUrl();
  }

  public String getLoginUri() {
    return url.getLoginUri();
  }

  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  private static class ICDUrl {

    private static final String BASE_URL_PATTERN = "%s%s.%s";
    private static final String LOGIN_URI_PATTERN =
        "/api/%d/signmein?useraccess=%s&access_token=%s";

    private String protocol;
    private String domain;
    private String subdomain;
    private int me;
    private String userAccess;
    private String app;

    String getBaseUrl() {
      return
          String.format(BASE_URL_PATTERN, protocol, subdomain, domain);
    }

    String getLoginUri() {
      return
          String.format(LOGIN_URI_PATTERN, me, userAccess, app);
    }
  }
}
