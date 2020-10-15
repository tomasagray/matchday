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

package self.me.matchday.plugin.datasource.zkfootball;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.datasource.bloggerparser.BloggerParserPatterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Container class for variables specific to the ZKFootball Blogger blog, needed for parsing. */
@EqualsAndHashCode(callSuper = true)
@Data
@Configuration
@PropertySource("classpath:plugins/zkf/zkf.patterns.properties")
@ConfigurationProperties(prefix = "zkf.patterns")
public class ZKFPatterns extends BloggerParserPatterns {

  // Video metadata
  private String metadata;
  private String resolution;
  private String framerate;
  private String container;
  private long defaultBitrate;
  private String mbps;
  private String kbps;

  /**
   * Does the supplied text String contain EventFileSource metadata?
   *
   * @param text HTML text containing ZKF EventFileSource metadata
   * @return True / false
   */
  public boolean isMetadata(@NotNull final String text) {
    return Pattern.compile(metadata).matcher(text).find();
  }

  public Matcher getResolutionMatcher(@NotNull final String data) {
    return Pattern.compile(resolution, Pattern.CASE_INSENSITIVE).matcher(data);
  }

  public Matcher getFramerateMatcher(@NotNull final String data) {
    return Pattern.compile(framerate, Pattern.CASE_INSENSITIVE).matcher(data);
  }

  public Matcher getContainerMatcher(@NotNull final String data) {
    return Pattern.compile(container, Pattern.CASE_INSENSITIVE).matcher(data);
  }

  public Matcher getMbpsMatcher(@NotNull final String data) {
    return Pattern.compile(mbps, Pattern.CASE_INSENSITIVE).matcher(data);
  }

  public Matcher getKbpsMatcher(@NotNull final String data) {
    return Pattern.compile(kbps, Pattern.CASE_INSENSITIVE).matcher(data);
  }
}
