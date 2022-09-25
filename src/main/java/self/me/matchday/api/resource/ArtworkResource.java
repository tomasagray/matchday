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

import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.ArtworkController;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.ArtworkCollection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "artwork")
@Relation(collectionRelation = "artworks")
public class ArtworkResource extends RepresentationModel<ArtworkResource> {

  private Long id;
  private Long fileSize;
  private String mediaType;
  private int width;
  private int height;
  private LocalDateTime created;
  private LocalDateTime modified;
  private boolean selected;

  @Component
  public static class ArtworkResourceAssembler
      extends RepresentationModelAssemblerSupport<Artwork, ArtworkResource> {

    public ArtworkResourceAssembler() {
      super(ArtworkController.class, ArtworkResource.class);
    }

    @Override
    public @NotNull ArtworkResource toModel(@NotNull Artwork entity) {
      final ArtworkResource resource = instantiateModel(entity);
      resource.setId(entity.getId());
      resource.setFileSize(entity.getFileSize());
      resource.setMediaType(entity.getMediaType());
      resource.setWidth(entity.getWidth());
      resource.setHeight(entity.getHeight());
      resource.setCreated(entity.getCreated());
      resource.setModified(entity.getModified());
      return resource;
    }

    public CollectionModel<ArtworkResource> fromCollection(@NotNull ArtworkCollection collection) {

      if (collection.size() > 0) {
        final Long selectedId = collection.getSelected().getId();
        final List<ArtworkResource> resources =
            collection.getCollection().stream()
                .map(this::toModel)
                .peek(resource -> resource.setSelected(resource.getId().equals(selectedId)))
                .collect(Collectors.toList());
        return CollectionModel.of(resources);
      }
      // else
      return CollectionModel.empty();
    }
  }
}
