package self.me.matchday.feed;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import self.me.matchday.model.EventFile.EventPartIdentifier;

/**
 * Represents a collection of files which compose an Event. Includes metadata describing the media
 * stream and its origin.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class EventFileSource {

  @Id @GeneratedValue protected Long eventFileSrcId;
  public Long getEventFileSrcId() {
    return eventFileSrcId;
  }

  // Fields
  protected String channel;
  protected String source;
  protected String approximateDuration;
  protected String approximateFileSize;
  protected Resolution resolution;
  @ElementCollection protected List<String> languages;
  @ElementCollection protected List<String> videoData;
  @ElementCollection protected List<String> audioData;
  @ElementCollection protected Map<URL, EventPartIdentifier> urls;

  public abstract String getChannel();

  public abstract String getSource();

  public abstract String getApproximateDuration();

  public abstract String getApproximateFileSize();

  public abstract Resolution getResolution();

  public abstract List<String> getLanguages();

  public abstract List<String> getVideoData();

  public abstract List<String> getAudioData();

  public abstract Map<URL, EventPartIdentifier> getUrls();

  public enum Resolution {
    R_4k("4K", 3840, 2160),
    R_1080p("1080p", 1920, 1080),
    R_1080i("1080i", 1920, 1080),
    R_720p("720p", 1280, 720),
    R_576p("576p", 768, 576),
    R_SD("SD", 640, 480);

    // Fields
    private final String name;
    private final Pattern pattern;
    private final int width;
    private final int height;

    @Contract(pure = true)
    Resolution(@NotNull String name, int width, int height) {
      this.name = name;
      this.pattern = Pattern.compile(".*" + name + ".*");
      this.width = width;
      this.height = height;
    }

    @Contract(pure = true)
    public String getName() {
      return this.name;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    /**
     * Determines if the supplied String corresponds to an enumerated video resolution.
     *
     * @param str The test String
     * @return True / false
     */
    public static boolean isResolution(@NotNull String str) {
      return Arrays.stream(Resolution.values())
          .map(resolution -> resolution.pattern.matcher(str))
          .anyMatch(Matcher::matches);
    }

    /**
     * Factory method to return an enumerated video resolution from a given String.
     *
     * @param str The String to be converted.
     * @return The Resolution value, or <b>null</b> if the given String does not match an enumerated
     *     value.
     */
    @Nullable
    @Contract(pure = true)
    public static Resolution fromString(@NotNull String str) {
      return Arrays.stream(Resolution.values())
          .filter(resolution -> resolution.pattern.matcher(str).matches())
          .findFirst()
          .orElse(null);
    }
  }
}
