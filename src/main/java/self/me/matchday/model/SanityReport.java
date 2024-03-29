package self.me.matchday.model;

import java.sql.Timestamp;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Data
@Builder
public final class SanityReport {

  private final ArtworkSanityReport artworkSanityReport;
  private final VideoSanityReport videoSanityReport;
  private final Timestamp timestamp;

  @Data
  @Builder
  public static final class ArtworkSanityReport {
    private final List<String> danglingFiles;
    private final List<Artwork> danglingDbEntries;
    private long totalFiles;
    private long totalDbEntries;
  }

  @Data
  @Builder
  public static final class VideoSanityReport {
    private final List<VideoStreamLocator> danglingStreamLocators;
    private final List<VideoStreamLocatorPlaylist> danglingPlaylists;
    private long totalStreamLocators;
    private long totalLocatorPlaylists;
  }
}
