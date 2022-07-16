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

package self.me.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.model.video.VideoFileSource;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video-source")
@Relation(collectionRelation = "video-sources", itemRelation = "video-source")
@JsonInclude(value = Include.NON_NULL)
public class VideoResource extends RepresentationModel<VideoResource> {

  private static final LinkRelation MASTER_PLAYLIST = LinkRelation.of("direct_master");
  private static final LinkRelation VARIANT_PLAYLIST = LinkRelation.of("direct_variant");
  private static final LinkRelation TRANSCODE_STREAM = LinkRelation.of("transcode_stream");
  private static final LinkRelation TRANSCODE_PLS_STREAM = LinkRelation.of("transcode_pls_stream");

  private UUID id;
  private String channel;
  private String source;
  private String languages;
  private String resolution;
  private String mediaContainer;
  private String bitrate;
  private Integer frameRate;
  private String videoCodec;
  private String audioCodec;
  private String duration;

  @Component
  public static class VideoResourceAssembler
      extends RepresentationModelAssemblerSupport<VideoFileSource, VideoResource> {

    @Getter @Setter private UUID eventId;

    public VideoResourceAssembler() {
      super(VideoStreamingController.class, VideoResource.class);
    }

    @Override
    public @NotNull VideoResource toModel(@NotNull VideoFileSource entity) {

      final VideoResource videoResource = instantiateModel(entity);

      final UUID fileSrcId = entity.getFileSrcId();
      final int framerate = entity.getFramerate();
      // Add metadata
      videoResource.setId(fileSrcId);
      videoResource.setChannel(entity.getChannel());
      videoResource.setSource(entity.getSource());
      videoResource.setLanguages(entity.getLanguages());
      videoResource.setResolution(entity.getVideoCodec());
      videoResource.setMediaContainer(entity.getMediaContainer());
      videoResource.setBitrate(entity.getVideoBitrate() + "Mbps");
      if (framerate > 0) {
        videoResource.setFrameRate(framerate);
      }
      videoResource.setVideoCodec(entity.getVideoCodec());
      videoResource.setAudioCodec(entity.getAudioCodec());
      videoResource.setDuration(entity.getApproximateDuration());

      // remote stream (no transcoding)
      videoResource.add(
          linkTo(
                  methodOn(VideoStreamingController.class)
                      .getVideoStreamPlaylist(eventId, fileSrcId))
              .withRel(VARIANT_PLAYLIST));

      // local stream (transcoded to local disk)
      videoResource.add(
          linkTo(
                  methodOn(VideoStreamingController.class)
                      .getVideoStreamPlaylist(eventId, fileSrcId))
              .withRel(TRANSCODE_STREAM));

      // locally transcoded stream (.pls format)
      videoResource.add(
          linkTo(
                  methodOn(VideoStreamingController.class)
                      .getVideoStreamPlsPlaylist(eventId, fileSrcId))
              .withRel(TRANSCODE_PLS_STREAM));
      return videoResource;
    }

    @Override
    public @NotNull CollectionModel<VideoResource> toCollectionModel(
        @NotNull Iterable<? extends VideoFileSource> entities) {

      final CollectionModel<VideoResource> videoResources = super.toCollectionModel(entities);

      // Add link to master playlist
      videoResources.add(
          linkTo(methodOn(VideoStreamingController.class).getMasterPlaylist(eventId))
              .withRel(MASTER_PLAYLIST));
      return videoResources;
    }
  }
}
