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
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.VideoFileSourceResource;
import self.me.matchday.api.resource.VideoFileSourceResource.VideoFileSourceResourceAssembler;
import self.me.matchday.api.resource.VideoPlaylistResource;
import self.me.matchday.api.resource.VideoPlaylistResource.VideoPlaylistResourceAssembler;
import self.me.matchday.api.service.video.VideoStreamingService;
import self.me.matchday.model.video.VideoPlaylist;

@RestController
@RequestMapping(value = "/events/event/{eventId}/video")
public class VideoStreamingController {

  public static final String MEDIA_TYPE_APPLE_MPEGURL = "application/vnd.apple.mpegurl";

  private final VideoStreamingService streamingService;
  private final VideoFileSourceResourceAssembler resourceAssembler;
  private final VideoPlaylistResourceAssembler playlistResourceAssembler;

  public VideoStreamingController(
          final VideoStreamingService streamingService,
          final VideoFileSourceResourceAssembler resourceAssembler,
          final VideoPlaylistResourceAssembler playlistResourceAssembler) {

    this.streamingService = streamingService;
    this.resourceAssembler = resourceAssembler;
    this.playlistResourceAssembler = playlistResourceAssembler;
  }

  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<VideoFileSourceResource>> getVideoResources(
      @PathVariable final UUID eventId) {

    resourceAssembler.setEventId(eventId);
    return streamingService
        .fetchVideoFileSources(eventId)
        .map(resourceAssembler::toCollectionModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/playlist/master",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<VideoPlaylist> getMasterPlaylist(@PathVariable final UUID eventId) {

    return streamingService
        .getBestVideoStreamPlaylist(eventId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/playlist",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<VideoPlaylistResource> getVideoStreamPlaylist(
      @PathVariable("eventId") UUID eventId, @PathVariable("fileSrcId") UUID fileSrcId) {

    return streamingService
        .getVideoStreamPlaylist(eventId, fileSrcId)
        .map(playlistResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> {
          final String errMsg =
                  String.format("Unable to stream Event: %s, VideoFileSource: %s", eventId, fileSrcId);
          return new IllegalArgumentException(errMsg);
        });
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/{partId}/playlist.m3u8",
      method = RequestMethod.GET,
      produces = MEDIA_TYPE_APPLE_MPEGURL)
  public ResponseEntity<String> getVideoPartPlaylist(
      @PathVariable("eventId") UUID eventId,
      @PathVariable("fileSrcId") UUID fileSrcId,
      @PathVariable("partId") Long partId) {

    final Optional<String> playlistFile = streamingService.readPlaylistFile(partId);
    return ResponseEntity.of(playlistFile);
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/{partId}/{segmentId}.ts",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> getSegmentFile(
      @PathVariable("eventId") UUID eventId,
      @PathVariable("fileSrcId") UUID fileSrcId,
      @PathVariable("partId") Long partId,
      @PathVariable("segmentId") String segmentId) {

    final Resource videoSegmentResource =
        streamingService.getVideoSegmentResource(partId, segmentId);
    return videoSegmentResource != null
        ? ResponseEntity.ok(videoSegmentResource)
        : ResponseEntity.notFound().build();
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/delete-stream",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> deleteVideoData(
      @PathVariable("eventId") UUID eventId, @PathVariable("fileSrcId") UUID fileSrcId)
      throws IOException {

    streamingService.deleteVideoData(fileSrcId);
    final String message =
        String.format("Deleted stream data for Event: %s, File Source: %s", eventId, fileSrcId);
    return ResponseEntity.ok(message);
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/kill-stream",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> killStreamTasks(
      @PathVariable("eventId") UUID eventId, @PathVariable("fileSrcId") UUID fileSrcId) {

    final int killedTasks = streamingService.killAllStreamingTasks();
    final String message =
        String.format(
            "Killed %s streaming tasks for Event: %s, file source: %s",
            killedTasks, eventId, fileSrcId);
    return ResponseEntity.ok(message);
  }

  @ExceptionHandler(IOException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  public String handleIoError(@NotNull IOException e) {
    return e.getMessage();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ResponseBody
  public String handleBadArgument(@NotNull IllegalArgumentException e) {
    return e.getMessage();
  }
}
