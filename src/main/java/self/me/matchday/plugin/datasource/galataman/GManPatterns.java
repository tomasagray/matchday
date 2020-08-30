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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Container class for Galataman parsing patterns
 */
@Data
@Configuration
@PropertySource("classpath:plugins/gman/gman.patterns.properties")
@ConfigurationProperties(prefix = "gman.patterns")
public class GManPatterns {

  // Video metadata
  private String startOfMetadata;
  private String metadataDelimiter;
  private String metadataKvDelimiter;
  private String languageDelimiter;
  private String filesize;
  private String channel;
  private String bitrate;
  private Long bitrateConversionFactor;
  private String container;
  private String framerate;

  // Event metadata
  private String competition;
  private String fixture;
  private String teams;
  private String date;
  private String season;
  private String fileLink;

  public String getAvDataDelimiter() {
    return
        Pattern.compile("‖").pattern();
  }

  public Matcher getFilesizeMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(filesize, Pattern.CASE_INSENSITIVE)
            .matcher(data);
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

  public Matcher getFileLinkMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(fileLink, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getCompetitionMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(competition, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getSeasonMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(season, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getFixtureMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(fixture, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getDateMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(date, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getTeamsMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(teams, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public boolean isSourceData(@NotNull final Element elem) {
    return ("b".equals(elem.tagName())) && (elem.text().contains("Channel"));
  }

  public boolean isVideoLink(@NotNull final Element elem) {

    final String href = elem.attr("href");
    return
        ("a".equals(elem.tagName())) && (getFileLinkMatcher(href).find());
  }
}
