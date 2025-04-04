package net.tomasbot.matchday.api.service.admin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.model.VideoSanityReport;
import net.tomasbot.matchday.model.VideoSanityReport.DanglingLocatorPlaylist;
import net.tomasbot.matchday.model.VideoSanityReport.DanglingVideoStreamLocator;
import net.tomasbot.matchday.model.VideoSanityReport.VideoSanityReportBuilder;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoSanityService {

  private final VideoStreamingService streamingService;

  public VideoSanityService(VideoStreamingService streamingService) {
    this.streamingService = streamingService;
  }

  @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
  public VideoSanityReport createVideoSanityReport() {
    final VideoSanityReportBuilder reportBuilder = VideoSanityReport.builder();

    int playlistCount = streamingService.fetchAllPlaylists().size();
    reportBuilder.totalLocatorPlaylists(playlistCount);
    int locatorCount = streamingService.fetchAllVideoStreamLocators().size();
    reportBuilder.totalStreamLocators(locatorCount);

    List<DanglingLocatorPlaylist> danglingPlaylists = findDanglingLocatorPlaylists();
    reportBuilder.danglingPlaylists(danglingPlaylists);
    List<DanglingVideoStreamLocator> danglingLocators = findDanglingVideoStreamLocators();
    reportBuilder.danglingStreamLocators(danglingLocators);

    return reportBuilder.build();
  }

  /**
   * Find any VideoStreamLocatorPlaylists which refer to non-existent storage locations.
   *
   * @return the updated report
   */
  private @NotNull List<DanglingLocatorPlaylist> findDanglingLocatorPlaylists() {
    final List<DanglingLocatorPlaylist> danglingPlaylists = new ArrayList<>();
    final List<VideoStreamLocatorPlaylist> playlists = streamingService.fetchAllPlaylists();

    // check for existence of video storage location
    for (final VideoStreamLocatorPlaylist playlist : playlists) {
      final Path storageLocation = playlist.getStorageLocation();
      if (!storageLocation.toFile().exists()) {
        danglingPlaylists.add(new DanglingLocatorPlaylist(playlist));
      }
    }

    return danglingPlaylists;
  }

  /**
   * Find any VideoStreamLocators which do not have a corresponding playlist file on the filesystem.
   *
   * @return The updated report builder
   */
  private @NotNull List<DanglingVideoStreamLocator> findDanglingVideoStreamLocators() {
    final List<DanglingVideoStreamLocator> danglingLocators = new ArrayList<>();
    final List<VideoStreamLocator> streamLocators = streamingService.fetchAllVideoStreamLocators();

    // check existence of playlist for each locator
    for (final VideoStreamLocator locator : streamLocators) {
      final Path playlistPath = locator.getPlaylistPath();
      if (!playlistPath.toFile().exists()) {
        DanglingVideoStreamLocator dangler = new DanglingVideoStreamLocator(locator);
        danglingLocators.add(dangler);
      }
    }

    return danglingLocators;
  }
}
