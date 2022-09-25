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
import self.me.matchday.api.resource.ArtworkResource.ArtworkResourceAssembler;
import self.me.matchday.model.ArtworkCollection;
import self.me.matchday.model.ArtworkRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "artwork_collection")
@Relation(collectionRelation = "artwork_collections")
public class ArtworkCollectionResource extends RepresentationModel<ArtworkCollectionResource> {

  private Long id;
  private ArtworkRole role;
  private int selectedIndex;
  private CollectionModel<ArtworkResource> artwork;

  @Component
  public static class ArtworkCollectionResourceAssembler
      extends RepresentationModelAssemblerSupport<ArtworkCollection, ArtworkCollectionResource> {

    private final ArtworkResourceAssembler artworkModeller;

    public ArtworkCollectionResourceAssembler(ArtworkResourceAssembler artworkModeller) {
      super(ArtworkController.class, ArtworkCollectionResource.class);
      this.artworkModeller = artworkModeller;
    }

    @Override
    public @NotNull ArtworkCollectionResource toModel(@NotNull ArtworkCollection collection) {
      ArtworkCollectionResource model = instantiateModel(collection);
      model.setId(collection.getId());
      model.setRole(collection.getRole());
      model.setSelectedIndex(collection.getSelectedIndex());
      model.setArtwork(artworkModeller.fromCollection(collection));
      return model;
    }
  }
}
