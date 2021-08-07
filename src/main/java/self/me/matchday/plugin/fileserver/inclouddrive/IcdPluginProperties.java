/*
 * Copyright (c) 2021.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.fileserver.inclouddrive;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

import java.net.MalformedURLException;
import java.net.URL;

@EqualsAndHashCode(callSuper = true)
@Data
@Configuration
@PropertySource("classpath:plugins/icd/icd.properties")
@ConfigurationProperties(prefix = "plugin.icd")
public class IcdPluginProperties extends PluginProperties {

  private String urlPattern;
  private String linkIdentifier;
  private String userDataIdentifier;

  @Value("${plugin.icd.user-agent.mac}") // mac & win versions in properties file
  private String userAgent;

  private ICDUrl url;
  private int defaultRefreshHours;

  public URL getBaseUrl() {
    final String baseUrl = url.getBaseUrl();
    try {
      return new URL(baseUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Could not parse InCloudDrive base URL: " + baseUrl);
    }
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
      return String.format(BASE_URL_PATTERN, protocol, subdomain, domain);
    }

    String getLoginUri() {
      return String.format(LOGIN_URI_PATTERN, me, userAccess, app);
    }
  }
}
