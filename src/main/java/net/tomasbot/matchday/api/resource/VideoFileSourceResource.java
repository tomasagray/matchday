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

package net.tomasbot.matchday.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.matchday.api.controller.EventController;
import net.tomasbot.matchday.api.controller.VideoStreamingController;
import net.tomasbot.matchday.api.resource.VideoFileResource.VideoFileResourceModeller;
import net.tomasbot.matchday.db.EventRepository;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.video.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video-source")
@Relation(collectionRelation = "video-sources", itemRelation = "video-source")
public class VideoFileSourceResource extends RepresentationModel<VideoFileSourceResource> {

  private static final LinkRelation PREFERRED_PLAYLIST = LinkRelation.of("preferred");
  private static final LinkRelation STREAM = LinkRelation.of("stream");
  private static final LinkRelation STREAM_REL = LinkRelation.of("video-stream");

  private UUID id;
  private String channel;
  private String source;
  private String languages;
  private String resolution;
  private String mediaContainer;
  private Integer frameRate;
  private Long videoBitrate;
  private String videoCodec;
  private Long audioBitrate;
  private String audioCodec;
  private String audioChannels;
  private String approximateDuration;
  private Long filesize;
  private Map<PartIdentifier, VideoFileResource> videoFiles;

  @Component
  public static class VideoSourceModeller
      extends EntityModeller<VideoFileSource, VideoFileSourceResource> {

    private final VideoFileResourceModeller videoFileModeller;
    private final EventRepository eventRepository;

    public VideoSourceModeller(
        VideoFileResourceModeller videoFileModeller, EventRepository eventRepository) {
      super(VideoStreamingController.class, VideoFileSourceResource.class);
      this.videoFileModeller = videoFileModeller;
      this.eventRepository = eventRepository;
    }

    @Override
    public @NotNull VideoFileSourceResource toModel(@NotNull VideoFileSource entity) {

      final VideoFileSourceResource resource = instantiateModel(entity);

      final UUID eventId = getEventId(entity);
      final UUID fileSrcId = entity.getFileSrcId();
      final int framerate = entity.getFramerate();
      final Resolution resolution = entity.getResolution();

      // Add metadata
      resource.setId(fileSrcId);
      resource.setChannel(entity.getChannel());
      resource.setSource(entity.getSource());
      resource.setLanguages(entity.getLanguages());
      resource.setMediaContainer(entity.getMediaContainer());
      resource.setVideoBitrate(entity.getVideoBitrate());
      resource.setVideoCodec(entity.getVideoCodec());
      resource.setFrameRate(25);
      resource.setAudioBitrate(entity.getAudioBitrate());
      resource.setAudioCodec(entity.getAudioCodec());
      resource.setAudioChannels(entity.getAudioChannels());
      resource.setApproximateDuration(entity.getApproximateDuration());
      resource.setFilesize(entity.getFilesize());
      resource.setVideoFiles(getVideoFiles(entity));
      if (resolution != null) {
        resource.setResolution(resolution.toString());
      }
      if (framerate > 0) {
        resource.setFrameRate(framerate);
      }

      resource.add(
          linkTo(methodOn(EventController.class).getVideoStreamPlaylist(eventId, fileSrcId))
              .withRel(STREAM));

      return resource;
    }

    @Nullable
    private UUID getEventId(@NotNull VideoFileSource entity) {
      return eventRepository.fetchEventForFileSource(entity).map(Event::getEventId).orElse(null);
    }

    private @Nullable Map<PartIdentifier, VideoFileResource> getVideoFiles(
        @NotNull VideoFileSource fileSource) {
      final UUID eventId = getEventId(fileSource);
      final UUID fileSrcId = fileSource.getFileSrcId();
      final List<VideoFilePack> filePacks = fileSource.getVideoFilePacks();

      if (filePacks != null && !filePacks.isEmpty()) {
        final VideoFilePack filePack =
            filePacks.get(0); // todo - should this be better? how to pick VideoFilePack?
        final Map<PartIdentifier, VideoFileResource> unsorted =
            filePack.allFiles().entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        entry -> {
                          VideoFile videoFile = entry.getValue();
                          UUID videoFileId = videoFile.getFileId();
                          VideoFileResource resource = videoFileModeller.toModel(videoFile);
                          resource.add(
                              linkTo(
                                      methodOn(EventController.class)
                                          .downloadVideoStream(eventId, fileSrcId, videoFileId))
                                  .withRel(STREAM_REL));
                          return resource;
                        }));
        return new TreeMap<>(unsorted);
      }
      return null;
    }

    public @NotNull CollectionModel<VideoFileSourceResource> toCollectionModel(
        @NotNull UUID eventId, @NotNull Iterable<? extends VideoFileSource> entities) {
      final CollectionModel<VideoFileSourceResource> resources = super.toCollectionModel(entities);

      // Add link to master playlist
      resources.add(
          linkTo(methodOn(EventController.class).getPreferredPlaylist(eventId))
              .withRel(PREFERRED_PLAYLIST));
      return resources;
    }

    private Map<PartIdentifier, VideoFile> getVideoFilesFromModel(
        @Nullable Map<PartIdentifier, VideoFileResource> resources) {
      if (resources == null) return null;
      return resources.entrySet().stream()
          .collect(Collectors.toMap(Entry::getKey, e -> videoFileModeller.fromModel(e.getValue())));
    }

    @Override
    public VideoFileSource fromModel(@Nullable VideoFileSourceResource resource) {
      if (resource == null) return null;
      final Resolution resolution = Resolution.fromString(resource.getResolution());
      final VideoFilePack videoFilePack = new VideoFilePack();
      videoFilePack.putAll(getVideoFilesFromModel(resource.getVideoFiles()));
      return VideoFileSource.builder()
          .fileSrcId(resource.getId())
          .channel(resource.getChannel())
          .source(resource.getSource())
          .languages(resource.getLanguages())
          .resolution(resolution)
          .mediaContainer(resource.getMediaContainer())
          .framerate(resource.getFrameRate())
          .videoBitrate(resource.getVideoBitrate())
          .videoCodec(resource.getVideoCodec())
          .audioBitrate(resource.getAudioBitrate())
          .audioCodec(resource.getAudioCodec())
          .audioChannels(resource.getAudioChannels())
          .approximateDuration(resource.getApproximateDuration())
          .filesize(resource.getFilesize())
          .videoFilePacks(List.of(videoFilePack))
          .build();
    }
  }
}
