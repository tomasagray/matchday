/*
 * Copyright (c) 2021.
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

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.model.video.VideoPlaylist;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonRootName("video-playlist")
@Relation(collectionRelation = "video-playlists", itemRelation = "video-playlist")
public class VideoPlaylistResource extends RepresentationModel<VideoPlaylistResource> {

  private String playlist;
  private long waitMillis;

  @Component
  public static class VideoPlaylistResourceAssembler
      extends RepresentationModelAssemblerSupport<VideoPlaylist, VideoPlaylistResource> {

    public VideoPlaylistResourceAssembler() {
      super(VideoStreamingController.class, VideoPlaylistResource.class);
    }

    @Override
    public @NotNull VideoPlaylistResource toModel(@NotNull final VideoPlaylist entity) {
      final VideoPlaylistResource playlistResource = instantiateModel(entity);
      playlistResource.setPlaylist(entity.getPlaylist());
      playlistResource.setWaitMillis(entity.getWaitMillis());
      playlistResource.add(
          linkTo(
                  methodOn(VideoStreamingController.class)
                      .getVideoStreamPlaylist(entity.getEventId(), entity.getFileSrcId()))
              .withSelfRel());
      return playlistResource;
    }
  }
}
