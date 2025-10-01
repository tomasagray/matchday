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

package net.tomasbot.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.IOException;
import java.util.UUID;
import net.tomasbot.matchday.api.resource.EventsResource;
import net.tomasbot.matchday.api.resource.EventsResource.EventsModeller;
import net.tomasbot.matchday.api.resource.VideoFileSourceResource;
import net.tomasbot.matchday.api.resource.VideoPlaylistResource;
import net.tomasbot.matchday.api.service.EventService;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class EventController {

  public static final LinkRelation NEXT_LINK = LinkRelation.of("next");

  private final EventService eventService;
  private final EventsModeller eventAssembler;
  private final VideoFileSourceResource.VideoSourceModeller fileSourceAssembler;
  private final VideoPlaylistResource.VideoPlaylistResourceAssembler playlistAssembler;

  EventController(
      EventService eventService,
      EventsModeller eventAssembler,
      VideoFileSourceResource.VideoSourceModeller fileSourceAssembler,
      VideoPlaylistResource.VideoPlaylistResourceAssembler playlistAssembler) {
    this.eventService = eventService;
    this.eventAssembler = eventAssembler;
    this.fileSourceAssembler = fileSourceAssembler;
    this.playlistAssembler = playlistAssembler;
  }

  @NotNull
  private static RuntimeException getPlaylistError(UUID fileSrcId) {
    final String msg = "Could not create playlist for VideoFileSource: " + fileSrcId;
    return new IllegalArgumentException(msg);
  }

  @ResponseBody
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET)
  public ResponseEntity<EventsResource> fetchAllEvents(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "16") int size) {
    Page<Event> events = eventService.fetchAllPaged(page, size);
    EventsResource resource = eventAssembler.toModel(events.getContent());

    if (events.hasNext())
      resource.add(
          linkTo(methodOn(EventController.class).fetchAllEvents(events.getNumber() + 1, size))
              .withRel(NEXT_LINK));

    return ResponseEntity.ok(resource);
  }

  // TODO: move to VideoStreamingController
  @RequestMapping(
      value = {"/event/{eventId}/video"},
      method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<VideoFileSourceResource>> getVideoResources(
      @PathVariable final UUID eventId) {
    return eventService
        .fetchVideoFileSources(eventId)
        .map(sources -> fileSourceAssembler.toCollectionModel(eventId, sources))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/event/{eventId}/video/stream/update",
      method = RequestMethod.PATCH,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<VideoFileSourceResource> updateVideoStream(
      @PathVariable UUID eventId, @RequestBody VideoFileSourceResource resource) {
    VideoFileSource source = fileSourceAssembler.fromModel(resource);
    VideoFileSource updated = eventService.updateVideoFileSource(eventId, source);
    VideoFileSourceResource model = fileSourceAssembler.toModel(updated);
    return ResponseEntity.ok(model);
  }

  @RequestMapping(
      value = "/event/{eventId}/video/stream/{fileSrcId}/delete",
      method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteVideoSource(
      @PathVariable UUID eventId, @PathVariable UUID fileSrcId) throws IOException {
    eventService.deleteVideoFileSource(eventId, fileSrcId);
    return ResponseEntity.ok().build();
  }

  @RequestMapping(
      value = "/event/{eventId}/video/playlist/preferred",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<VideoPlaylistResource> getPreferredPlaylist(
      @PathVariable final UUID eventId) {
    return eventService
        .getPreferredPlaylist(eventId)
        .map(playlistAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/event/{eventId}/video/stream/{fileSrcId}/playlist",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<VideoPlaylistResource> getVideoStreamPlaylist(
      @PathVariable("eventId") UUID eventId, @PathVariable("fileSrcId") UUID fileSrcId) {
    return eventService
        .getVideoStreamPlaylist(eventId, fileSrcId)
        .map(playlistAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> getPlaylistError(fileSrcId));
  }

  @RequestMapping(
      value = "/event/{eventId}/video/stream/{fileSrcId}/{videoFileId}/stream",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> downloadVideoStream(
      @PathVariable("eventId") UUID eventId,
      @PathVariable("fileSrcId") UUID fileSrcId,
      @PathVariable("videoFileId") UUID videoFileID) {
    return eventService
        .downloadVideoStream(eventId, fileSrcId, videoFileID)
        .map(playlistAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> getPlaylistError(fileSrcId));
  }
}
