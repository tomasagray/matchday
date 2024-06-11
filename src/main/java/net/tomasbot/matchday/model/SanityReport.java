package net.tomasbot.matchday.model;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;

@Data
@Builder
public final class SanityReport {

  private final ArtworkSanityReport artworkSanityReport;
  private final VideoSanityReport videoSanityReport;
  private final Timestamp timestamp;
  private final boolean requiresHealing;

  @AllArgsConstructor
  public static class DanglingVideoStreamLocator extends VideoStreamLocator {
    public DanglingVideoStreamLocator(@NotNull VideoStreamLocator locator) {
      this.streamLocatorId = locator.getStreamLocatorId();
      this.videoFile = locator.getVideoFile();
      this.playlistPath = locator.getPlaylistPath();
      this.state = locator.getState();
    }
  }

  @Data
  @Builder
  public static final class ArtworkSanityReport {
    private final List<Path> danglingFiles;
    private final List<Artwork> danglingDbEntries;
    private long totalFiles;
    private long totalDbEntries;
  }

  @Data
  @Builder
  public static final class VideoSanityReport {
    private final List<DanglingVideoStreamLocator> danglingStreamLocators;
    private final List<VideoStreamLocatorPlaylist> danglingPlaylists;
    private long totalStreamLocators;
    private long totalLocatorPlaylists;
  }
}
