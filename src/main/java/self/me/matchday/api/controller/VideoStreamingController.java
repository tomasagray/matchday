/*
 * Copyright (c) 2022.
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

package self.me.matchday.api.controller;

import java.io.IOException;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.service.video.VideoStreamingService;

@RestController
@RequestMapping(value = "/video")
public class VideoStreamingController {

  public static final String MEDIA_TYPE_APPLE_MPEGURL = "application/vnd.apple.mpegurl";

  private final VideoStreamingService streamingService;

  public VideoStreamingController(VideoStreamingService streamingService) {
    this.streamingService = streamingService;
  }

  @RequestMapping(
      value = "/part/{partId}/playlist.m3u8",
      method = RequestMethod.GET,
      produces = MEDIA_TYPE_APPLE_MPEGURL)
  public ResponseEntity<String> getVideoPartPlaylist(@PathVariable("partId") Long partId)
      throws Exception {
    final String playlistFile = streamingService.readPlaylistFile(partId);
    return ResponseEntity.ok(playlistFile);
  }

  @RequestMapping(
      value = "/part/{partId}/{segmentId}.ts",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> getSegmentFile(
      @PathVariable("partId") Long partId, @PathVariable("segmentId") String segmentId) {
    Resource videoSegmentResource = streamingService.getVideoSegmentResource(partId, segmentId);
    return videoSegmentResource != null
        ? ResponseEntity.ok(videoSegmentResource)
        : ResponseEntity.notFound().build();
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/kill-streams",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Integer> killStreamTasks(@PathVariable("fileSrcId") UUID fileSrcId) {
    return ResponseEntity.ok(streamingService.killAllStreamsFor(fileSrcId));
  }

  @RequestMapping(
      value = "/stream/{videoFileId}/kill-stream",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UUID> killStream(@PathVariable("videoFileId") UUID videoFileId) {
    streamingService.killStreamFor(videoFileId);
    return ResponseEntity.ok(videoFileId);
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/delete-streams",
      method = RequestMethod.DELETE,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UUID> deleteVideoData(@PathVariable("fileSrcId") UUID fileSrcId)
      throws IOException {
    streamingService.deleteAllVideoData(fileSrcId);
    return ResponseEntity.ok(fileSrcId);
  }

  @RequestMapping(
      value = "/stream/{videoFileId}/delete-stream",
      method = RequestMethod.DELETE,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UUID> deleteStream(@PathVariable("videoFileId") UUID videoFileId)
      throws IOException {
    streamingService.deleteVideoData(videoFileId);
    return ResponseEntity.ok(videoFileId);
  }
}
