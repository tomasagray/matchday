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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.tomasbot.matchday.api.controller.VideoStreamingController;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.model.video.PartIdentifier;
import net.tomasbot.matchday.model.video.VideoPlaylist;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@JsonRootName(value = "video-playlist")
@Relation(collectionRelation = "video-playlists", itemRelation = "video-playlist")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class VideoPlaylistResource extends RepresentationModel<VideoPlaylistResource> {

  private final List<Pair> uris = new ArrayList<>();

  public void addUri(@NotNull String title, @NotNull URI uri) {
    uris.add(new Pair(title, uri));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoPlaylistResource that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(getUris(), that.getUris());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getUris());
  }

  public record Pair(String title, URI uri) {}

  @Component
  public static class VideoPlaylistResourceAssembler
      extends RepresentationModelAssemblerSupport<VideoPlaylist, VideoPlaylistResource> {

    VideoPlaylistResourceAssembler() {
      super(VideoStreamingService.class, VideoPlaylistResource.class);
    }

    private static void addPlaylistLink(
        @NotNull VideoPlaylistResource resource, @NotNull Entry<Long, PartIdentifier> entry) {
      try {
        final Long locatorId = entry.getKey();
        final PartIdentifier partId = entry.getValue();
        final URI playlistUri =
            linkTo(methodOn(VideoStreamingController.class).getVideoPartPlaylist(locatorId))
                .withSelfRel()
                .toUri();
        final String title = partId != null ? partId.getPartName() : "";
        resource.addUri(title, playlistUri);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public @NotNull VideoPlaylistResource toModel(@NotNull VideoPlaylist playlist) {

      final VideoPlaylistResource resource = instantiateModel(playlist);
      final List<Entry<Long, PartIdentifier>> locatorIds =
          new ArrayList<>(playlist.getLocatorIds().entrySet());
      locatorIds.sort(Entry.comparingByValue());
      locatorIds.forEach(entry -> addPlaylistLink(resource, entry));
      return resource;
    }
  }
}
