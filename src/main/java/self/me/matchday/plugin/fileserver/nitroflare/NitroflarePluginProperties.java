/*
 * Copyright (c) 2020.
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

package self.me.matchday.plugin.fileserver.nitroflare;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

@EqualsAndHashCode(callSuper = true)
@Data
@Configuration
@PropertySource("classpath:plugins/nitroflare/nitroflare.properties")
@ConfigurationProperties(prefix = "plugin.nitroflare")
public class NitroflarePluginProperties extends PluginProperties{

  private String baseUrl;
  private String loginUrl;
  private String downloadLinkId;
  private int refreshRateHours;
  @Value("${plugin.nitroflare.url-pattern.regexp}")
  private String urlPattern;
  private String userAgent;

}
