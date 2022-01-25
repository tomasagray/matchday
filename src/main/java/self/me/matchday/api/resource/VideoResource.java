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
import self.me.matchday.model.video.Resolution;
import self.me.matchday.model.video.VideoFileSource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video-resource")
@Relation(collectionRelation = "video-resources", itemRelation = "video-resource")
@JsonInclude(value = Include.NON_NULL)
public class VideoResource extends RepresentationModel<VideoResource> {

  private static final LinkRelation MASTER_PLAYLIST = LinkRelation.of("direct_master");
  private static final LinkRelation VARIANT_PLAYLIST = LinkRelation.of("direct_variant");
  private static final LinkRelation TRANSCODE_STREAM = LinkRelation.of("transcode_stream");
  private static final LinkRelation TRANSCODE_PLS_STREAM = LinkRelation.of("transcode_pls_stream");

  private String channel;
  private String source;
  private String languages;
  private String resolution;
  private String mediaContainer;
  private Long bitrate;
  private int frameRate;
  private String videoCodec;
  private String audioCodec;

  @Component
  public static class VideoResourceAssembler
      extends RepresentationModelAssemblerSupport<VideoFileSource, VideoResource> {

    @Getter @Setter private String eventId;

    public VideoResourceAssembler() {
      super(VideoStreamingController.class, VideoResource.class);
    }

    @Override
    public @NotNull VideoResource toModel(@NotNull VideoFileSource entity) {

      final VideoResource videoResource = instantiateModel(entity);

      final String fileSrcId = entity.getFileSrcId();
      final Resolution resolution = entity.getResolution();

      // Add metadata
      videoResource.channel = entity.getChannel();
      videoResource.source = entity.getSource();
      videoResource.languages = entity.getLanguages();
      videoResource.resolution = (resolution != null) ? resolution.toString() : null;
      videoResource.mediaContainer = entity.getMediaContainer();
      videoResource.bitrate = entity.getVideoBitrate();
      videoResource.frameRate = entity.getFramerate();
      videoResource.videoCodec = entity.getVideoCodec();
      videoResource.audioCodec = entity.getAudioCodec();

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
