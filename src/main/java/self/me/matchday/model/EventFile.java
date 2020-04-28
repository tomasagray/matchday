/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.net.URL;
import java.util.Comparator;
import java.util.regex.Pattern;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.converter.VideoMetadataConverter;

@Entity
@Data
@NoArgsConstructor
public class EventFile {

  private static double DEFAULT_DURATION = 3012.541956d;

  // Fields
  @Id
  @GeneratedValue
  private Long eventFileId;
  private EventPartIdentifier title;
  private URL externalUrl;
  // refreshed data
  @Column(columnDefinition = "LONGTEXT")
  private URL internalUrl;
  @Convert(converter = VideoMetadataConverter.class)
  @Column(columnDefinition = "LONGTEXT")
  private VideoMetadata metadata;

  public EventFile(@NotNull final EventPartIdentifier title, @NotNull final URL externalUrl) {

    this.title = title;
    this.externalUrl = externalUrl;
    this.internalUrl = null;
    this.metadata = null;
  }

  /**
   * Returns the duration of this EventFile, in milliseconds.
   *
   * @return The duration of this EventFile (millis).
   */
  public double getDuration() {
    if (getMetadata() != null) {
      return getMetadata().getFormat().getDuration();
    } else {
      return DEFAULT_DURATION;
    }
  }

  public String toString() {
    return String.format("%s - %s", getTitle(), getExternalUrl().toString());
  }

  /**
   * Event part identifiers
  */
  public enum EventPartIdentifier {

    DEFAULT("", "", -1),
    PRE_MATCH("Pre-Match", "^[Pp][Rr][Ee][- ][Mm][Aa][Tt][Cc][Hh]$", 0),
    FIRST_HALF("1st Half", "1 ?[Ss][Tt] [Hh][Aa][Ll][Ff]", 1),
    SECOND_HALF("2nd Half", "2 ?[Nn][Dd] [Hh][Aa][Ll][Ff]", 2),
    EXTRA_TIME("Extra-Time/Penalties", "^[Ee][Xx][Tt][Rr][Aa][- ][Tt][Ii][Mm][Ee]", 3),
    TROPHY_CEREMONY("Trophy Ceremony", "^[Tt][Rr][Oo][Pp][Hh][Yy]", 4),
    POST_MATCH("Post-Match", "^[Pp][Oo][Ss][Tt][- ][Mm][Aa][Tt][Cc][Hh]$", 5);

    private final String name;
    private final Pattern pattern;
    private final int order;

    EventPartIdentifier(@NotNull String name, @NotNull String pattern, int order) {
      this.name = name;
      this.pattern = Pattern.compile(pattern);
      this.order = order;
    }

    @Override
    public String toString() {
      return this.name;
    }

    /**
     * Determines if the given String corresponds to an enumerated Event part identifier.
     * @param str The test String
     * @return True / false.
     */
    public static boolean isPartIdentifier(@NotNull String str) {
      return PRE_MATCH.pattern.matcher(str).find()
          || FIRST_HALF.pattern.matcher(str).find()
          || SECOND_HALF.pattern.matcher(str).find()
          || EXTRA_TIME.pattern.matcher(str).find()
          || TROPHY_CEREMONY.pattern.matcher(str).find()
          || POST_MATCH.pattern.matcher(str).find();
    }

    /**
     * Factory method to convert a String to an enumerated Event part identifier.
     * @param str The String to be converted.
     * @return The enumerated value, or <b>DEFAULT</b> if the given String does not match any values.
     */
    public static EventPartIdentifier fromString(@NotNull String str) {
      // If the given String doesn't match
      EventPartIdentifier result = DEFAULT;

      for (EventPartIdentifier partIdentifier : EventPartIdentifier.values()) {
        if (partIdentifier.pattern.matcher(str).matches()) {
          result = partIdentifier;
          break;
        }
      }

      return result;
    }
  }

  /**
   * Sorts EventFiles into correct order.
   */
  public static class EventFileSorter implements Comparator<EventFile> {

    @Override
    public int compare(@NotNull EventFile f1, @NotNull EventFile f2) {
      return f1.getTitle().order - f2.getTitle().order;
    }
  }
}
