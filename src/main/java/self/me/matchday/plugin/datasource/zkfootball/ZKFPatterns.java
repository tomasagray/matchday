package self.me.matchday.plugin.datasource.zkfootball;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Container class for variables specific to the ZKFootball Blogger blog, needed for parsing.
 */
@Data
@Configuration
@PropertySource("classpath:plugins/zkf/zkf.patterns.properties")
@ConfigurationProperties(prefix = "zkf.patterns")
public class ZKFPatterns {

  // Video metadata
  private String metadata;
  private String resolution;
  private String framerate;
  private String container;
  private String filesize;
  private long defaultBitrate;
  private String mbps;
  private String kbps;
  // Event metadata
  private String competition;
  private String season;
  private String fixture;
  private String teams;

  /**
   * Does the supplied text String contain EventFileSource metadata?
   *
   * @param text HTML text containing ZKF EventFileSource metadata
   * @return True / false
   */
  public boolean isMetadata(@NotNull final String text) {
    return
        Pattern
            .compile(metadata)
            .matcher(text)
            .find();
  }

  public Matcher getResolutionMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(resolution, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getFramerateMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(framerate, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getContainerMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(container, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getFilesizeMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(filesize, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getMbpsMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(mbps, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }

  public Matcher getKbpsMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(kbps, Pattern.CASE_INSENSITIVE)
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

  public Matcher getTeamsMatcher(@NotNull final String data) {
    return
        Pattern
            .compile(teams, Pattern.CASE_INSENSITIVE)
            .matcher(data);
  }
}
