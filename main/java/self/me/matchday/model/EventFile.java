/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.net.URL;
import java.util.Comparator;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Entity
@Data
public class EventFile {

  // Fields
  @Id
  @GeneratedValue
  private Long id;
  private final EventPartIdentifier title;
  private final URL uri;
  private final float duration;

  /**
   * Event part identifiers
  */
  public enum EventPartIdentifier {

    DEFAULT("", -1),
    PRE_MATCH("Pre-Match", 0),
    FIRST_HALF("1st Half", 1),
    SECOND_HALF("2nd Half", 2),
    EXTRA_TIME("Extra-Time/Penalties", 3),
    TROPHY_CEREMONY("Trophy Ceremony", 4),
    POST_MATCH("Post-Match", 5);

    private final Pattern pattern;
    private final int order;

    EventPartIdentifier(@NotNull String pattern, int order) {
      this.pattern = Pattern.compile(pattern);
      this.order = order;
    }

    @Override
    public String toString() {
      return this.pattern.toString();
    }

    /**
     * Determines if the given String corresponds to an enumerated Event part identifier.
     * @param str The test String
     * @return True / false
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
