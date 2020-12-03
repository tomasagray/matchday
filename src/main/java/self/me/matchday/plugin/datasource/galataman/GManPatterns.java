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

package self.me.matchday.plugin.datasource.galataman;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import self.me.matchday.plugin.datasource.bloggerparser.BloggerParserPatterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Container class for Galataman parsing patterns
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Configuration
@PropertySource("classpath:plugins/gman/gman.patterns.properties")
@ConfigurationProperties(prefix = "gman.patterns")
public class GManPatterns extends BloggerParserPatterns {

  // Video metadata
  private String startOfMetadata;
  private String metadataDelimiter;
  private String metadataKvDelimiter;
  private String languageDelimiter;
  private String channel;
  private String bitrate;
  private Long bitrateConversionFactor;
  private String container;
  private String framerate;

  public String getAvDataDelimiter() {
    return
        Pattern.compile("‖").pattern();
  }

  public Matcher getChannelMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(channel, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getBitrateMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(bitrate, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getContainerMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(container, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getFramerateMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(framerate, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

}
