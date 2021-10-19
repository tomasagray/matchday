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

package self.me.matchday.plugin.datasource.blogger;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.PluginProperties;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Setter
@Configuration
@PropertySource("classpath:plugins/blogger/blogger.properties")
@ConfigurationProperties(prefix = "plugin.blogger")
public class BloggerPluginProperties extends PluginProperties {

  private String bloggerUrlPattern;
  private String jsonUrlPattern;
  private String dateTimeFormatPattern;

  public Pattern getBloggerUrlPattern() {
    return Pattern.compile(bloggerUrlPattern);
  }

  public Pattern getJsonUrlPattern() {
    return Pattern.compile(jsonUrlPattern);
  }

  public DateTimeFormatter getDateTimeFormatter() {
    return DateTimeFormatter.ofPattern(dateTimeFormatPattern);
  }
}
