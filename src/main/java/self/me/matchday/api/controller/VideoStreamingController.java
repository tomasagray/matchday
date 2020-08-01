package self.me.matchday.api.controller;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.service.VideoStreamingService;

@RestController
@RequestMapping(value = "/videos")
public class VideoStreamingController {

  @Autowired
  private VideoStreamingService streamingService;

  @RequestMapping(value = "/{eventId}/{fileSrcId}/playlist.m3u8", method = RequestMethod.GET)
  ResponseEntity<String> getStreamPlaylist(@PathVariable("eventId") String eventId,
      @PathVariable("fileSrcId") UUID fileSrcId) {

    final String playlistFile = streamingService.readPlaylistFile(eventId, fileSrcId);
    return
        playlistFile != null && !("".equals(playlistFile)) ?
            ResponseEntity.ok(playlistFile) :
            ResponseEntity.notFound().build();
  }

  @RequestMapping(value = "/{eventId}/{fileSrcId}/{segmentId}.ts", method = RequestMethod.GET,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> getSegmentFile(@PathVariable("eventId") String eventId,
      @PathVariable("fileSrcId") UUID fileSrcId, @PathVariable("segmentId") String segmentId) {

    final Resource videoSegmentResource =
        streamingService.getVideoSegmentResource(eventId, fileSrcId, segmentId);
    return
        videoSegmentResource != null ?
            ResponseEntity.ok(videoSegmentResource) :
            ResponseEntity.notFound().build();
  }
}
