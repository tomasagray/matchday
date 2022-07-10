/*
 * Copyright (c) 2022.
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

package self.me.matchday.plugin.fileserver.filefox;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@Configuration
@PropertySource("classpath:plugins/filefox/filefox.properties")
@ConfigurationProperties(prefix = "plugin.filefox")
public class FileFoxPluginProperties extends PluginProperties {

  private String baseUrl;
  private String loginUrl;
  private String userAgent;
  private String linkUrlPattern;
  private String directDownloadUrlPattern;
  private String downloadLimitPattern;
  private int refreshHours;
  private String linkButtonText;
  private String premiumOnlyError;
  private String loggedOutText;
  private String ddlFormErrorText;

  public URL getBaseUrl() {
    try {
      return new URL(baseUrl);
    } catch (MalformedURLException e) {
      throw new FileFoxParsingException("Could not parse FileFox base URL: " + baseUrl, e);
    }
  }

  public URL getLoginUrl() {
    try {
      return new URL(loginUrl);
    } catch (MalformedURLException e) {
      throw new FileFoxParsingException("Could not parse FileFox login URL: " + loginUrl, e);
    }
  }

  public Pattern getLinkUrlPattern() {
    return Pattern.compile(linkUrlPattern);
  }

  public Pattern getDirectDownloadUrlPattern() {
    return Pattern.compile(directDownloadUrlPattern);
  }

  public Pattern getDownloadLimitPattern() {
    return Pattern.compile(downloadLimitPattern);
  }
}
