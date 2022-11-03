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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video-source")
@Relation(collectionRelation = "video-sources", itemRelation = "video-source")
@JsonInclude(value = Include.NON_NULL)
public class VideoFileSourceResource extends RepresentationModel<VideoFileSourceResource> {

  private static final LinkRelation PREFERRED_PLAYLIST = LinkRelation.of("preferred");
  private static final LinkRelation STREAM = LinkRelation.of("stream");

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
  public static class VideoFileSourceResourceAssembler
      extends RepresentationModelAssemblerSupport<VideoFileSource, VideoFileSourceResource> {

    @Getter @Setter private UUID eventId;

    public VideoFileSourceResourceAssembler() {
      super(VideoStreamingController.class, VideoFileSourceResource.class);
    }

    @Override
    public @NotNull VideoFileSourceResource toModel(@NotNull VideoFileSource entity) {

      final VideoFileSourceResource videoFileSourceResource = instantiateModel(entity);

      final UUID fileSrcId = entity.getFileSrcId();
      final int framerate = entity.getFramerate();
      final Resolution resolution = entity.getResolution();

      // Add metadata
      videoFileSourceResource.setId(fileSrcId);
      videoFileSourceResource.setChannel(entity.getChannel());
      videoFileSourceResource.setSource(entity.getSource());
      videoFileSourceResource.setLanguages(entity.getLanguages());
      videoFileSourceResource.setMediaContainer(entity.getMediaContainer());
      videoFileSourceResource.setBitrate(entity.getVideoBitrate() + "Mbps");
      videoFileSourceResource.setVideoCodec(entity.getVideoCodec());
      videoFileSourceResource.setAudioCodec(entity.getAudioCodec());
      videoFileSourceResource.setDuration(entity.getApproximateDuration());
      if (resolution != null) {
        videoFileSourceResource.setResolution(resolution.toString());
      }
      if (framerate > 0) {
        videoFileSourceResource.setFrameRate(framerate);
      }
      videoFileSourceResource.add(
          linkTo(
                  methodOn(VideoStreamingController.class)
                      .getVideoStreamPlaylist(eventId, fileSrcId))
              .withRel(STREAM));
      return videoFileSourceResource;
    }

    @Override
    public @NotNull CollectionModel<VideoFileSourceResource> toCollectionModel(
        @NotNull Iterable<? extends VideoFileSource> entities) {

      final CollectionModel<VideoFileSourceResource> videoResources =
          super.toCollectionModel(entities);

      // Add link to master playlist
      videoResources.add(
          linkTo(methodOn(VideoStreamingController.class).getPreferredPlaylist(eventId))
              .withRel(PREFERRED_PLAYLIST));
      return videoResources;
    }
  }
}
