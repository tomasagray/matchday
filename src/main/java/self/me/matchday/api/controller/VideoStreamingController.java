/*
 * Copyright (c) 2020.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.VideoResource;
import self.me.matchday.api.resource.VideoResource.VideoResourceAssembler;
import self.me.matchday.api.service.MasterPlaylistService;
import self.me.matchday.api.service.VariantPlaylistService;
import self.me.matchday.api.service.VideoStreamingService;
import self.me.matchday.model.M3UPlaylist;
import self.me.matchday.model.MasterM3U;
import self.me.matchday.util.Log;

import java.io.IOException;

@RestController
@RequestMapping(value = "/events/event/{eventId}/video")
public class VideoStreamingController {

  public static final String MEDIA_TYPE_MPEG_URL = "application/x-mpeg-url";

  private final VideoStreamingService streamingService;
  private final VideoResourceAssembler resourceAssembler;
  private final MasterPlaylistService masterPlaylistService;
  private final VariantPlaylistService variantPlaylistService;

  @Autowired
  public VideoStreamingController(
      final VideoStreamingService streamingService,
      final VideoResourceAssembler resourceAssembler,
      final MasterPlaylistService masterPlaylistService,
      final VariantPlaylistService variantPlaylistService) {

    this.streamingService = streamingService;
    this.resourceAssembler = resourceAssembler;
    this.masterPlaylistService = masterPlaylistService;
    this.variantPlaylistService = variantPlaylistService;
  }

  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<VideoResource>> getVideoResources(
      @PathVariable final String eventId) {

    // Set EventID for resource assembler
    resourceAssembler.setEventId(eventId);
    return streamingService
        .fetchEventFileSources(eventId)
        .map(resourceAssembler::toCollectionModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/playlist/master",
      method = RequestMethod.GET,
      produces = MEDIA_TYPE_MPEG_URL)
  public ResponseEntity<String> getMasterPlaylist(@PathVariable final String eventId) {

    return masterPlaylistService
        .fetchMasterPlaylistForEvent(eventId)
        .map(MasterM3U::toString)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/playlist/variant/{fileSrcId}",
      method = RequestMethod.GET,
      produces = MEDIA_TYPE_MPEG_URL)
  public ResponseEntity<String> getVariantPlaylist(
      @PathVariable final String eventId, @PathVariable final String fileSrcId) {

    Log.i(
        "VideoStreamingController",
        String.format(
            "Getting variant playlist for Event: %s, File Source: %s", eventId, fileSrcId));

    return variantPlaylistService
        .fetchVariantPlaylist(fileSrcId)
        .map(M3UPlaylist::toString)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/playlist.m3u8",
      method = RequestMethod.GET,
      produces = "application/vnd.apple.mpegurl")
  public ResponseEntity<String> getVideoStreamPlaylist(
      @PathVariable("eventId") String eventId, @PathVariable("fileSrcId") String fileSrcId) {

    return streamingService
        .getVideoStreamPlaylist(eventId, fileSrcId)
        .map(M3UPlaylist::getSimplePlaylist)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(value = "/stream/{fileSrcId}/playlist.pls", method = RequestMethod.GET)
  public ResponseEntity<String> getVideoStreamPlsPlaylist(
      @PathVariable("eventId") String eventId, @PathVariable("fileSrcId") String fileSrcId) {

    return streamingService
        .getVideoStreamPlaylist(eventId, fileSrcId)
        .map(M3UPlaylist::getPlsPlaylist)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/{partId}/playlist.m3u8",
      method = RequestMethod.GET,
      produces = "application/vnd.apple.mpegurl")
  public ResponseEntity<String> getVideoPartPlaylist(
      @PathVariable("eventId") String eventId,
      @PathVariable("fileSrcId") String fileSrcId,
      @PathVariable("partId") Long partId) {

    final String playlistFile = streamingService.readPlaylistFile(eventId, fileSrcId, partId);
    return playlistFile != null && !("".equals(playlistFile))
        ? ResponseEntity.ok(playlistFile)
        : ResponseEntity.notFound().build();
  }

  @RequestMapping(
      value = "/stream/{fileSrcId}/{partId}/{segmentId}.ts",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> getSegmentFile(
      @PathVariable("eventId") String eventId,
      @PathVariable("fileSrcId") String fileSrcId,
      @PathVariable("partId") Long partId,
      @PathVariable("segmentId") String segmentId) {

    final Resource videoSegmentResource =
        streamingService.getVideoSegmentResource(eventId, fileSrcId, partId, segmentId);
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
      @PathVariable("eventId") String eventId, @PathVariable("fileSrcId") String fileSrcId)
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
      @PathVariable("eventId") String eventId, @PathVariable("fileSrcId") String fileSrcId) {

    final int killedTasks = streamingService.killAllStreamingTasks();
    final String message = String.format("Killed %s streaming tasks", killedTasks);
    return ResponseEntity.ok(message);
  }
}
