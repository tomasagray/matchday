package net.tomasbot.matchday.api.service.admin;

import java.util.List;
import java.util.Optional;
import net.tomasbot.matchday.api.service.video.VideoStreamLocatorPlaylistService;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.model.VideoSanityReport;
import net.tomasbot.matchday.model.VideoSanityReport.DanglingLocatorPlaylist;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoHealingService {

  private final VideoSanityService sanityService;
  private final VideoStreamingService streamingService;
  private final VideoStreamLocatorPlaylistService playlistService;

  public VideoHealingService(
      VideoSanityService sanityService,
      VideoStreamingService streamingService,
      VideoStreamLocatorPlaylistService playlistService) {
    this.sanityService = sanityService;
    this.streamingService = streamingService;
    this.playlistService = playlistService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
  public VideoSanityReport autoHealVideos(@NotNull VideoSanityReport report) {
    List<DanglingLocatorPlaylist> danglingPlaylists = report.getDanglingPlaylists();
    for (DanglingLocatorPlaylist playlist : danglingPlaylists) {
      healDanglingPlaylist(playlist);
    }

    report.getDanglingStreamLocators().forEach(this::healDanglingLocator);

    return sanityService.createVideoSanityReport();
  }

  private void healDanglingPlaylist(@NotNull DanglingLocatorPlaylist danglingPlaylist) {
    Optional<VideoStreamLocatorPlaylist> playlistOptional =
        streamingService.getPlaylistForFileSource(danglingPlaylist.getFileSrcId());
    if (playlistOptional.isPresent()) {
      VideoStreamLocatorPlaylist playlist = playlistOptional.get();
      streamingService.deleteVideoStreamPlaylist(playlist);
    } else {
      throw new IllegalArgumentException(
          "Could not delete non-existent VideoStreamLocatorPlaylist: "
              + danglingPlaylist.getPlaylistId());
    }
  }

  private void healDanglingLocator(
      VideoSanityReport.@NotNull DanglingVideoStreamLocator danglingLocator) {
    Long locatorId = danglingLocator.getStreamLocatorId();
    streamingService
        .getVideoStreamLocator(locatorId)
        .ifPresent(
            locator -> {
              playlistService
                  .getVideoStreamPlaylistContaining(locatorId)
                  .ifPresent(playlist -> playlist.removeStreamLocator(locator));
              streamingService.deleteVideoStreamLocator(locator);
            });
  }
}
