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

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.matchday.api.controller.ArtworkController;
import net.tomasbot.matchday.api.resource.ArtworkResource.ArtworkModeller;
import net.tomasbot.matchday.model.Artwork;
import net.tomasbot.matchday.model.ArtworkCollection;
import net.tomasbot.matchday.model.ArtworkRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;

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
  private CollectionModel<ArtworkResource> collection;

  @Component
  public static class ArtworkCollectionModeller
      extends EntityModeller<ArtworkCollection, ArtworkCollectionResource> {

    private final ArtworkModeller artworkModeller;

    public ArtworkCollectionModeller(ArtworkModeller artworkModeller) {
      super(ArtworkController.class, ArtworkCollectionResource.class);
      this.artworkModeller = artworkModeller;
    }

    @Override
    public @NotNull ArtworkCollectionResource toModel(@NotNull ArtworkCollection collection) {
      ArtworkCollectionResource model = instantiateModel(collection);
      model.setId(collection.getId());
      model.setRole(collection.getRole());
      model.setSelectedIndex(collection.getSelectedIndex());
      model.setCollection(artworkModeller.fromCollection(collection));
      return model;
    }

    @Override
    public ArtworkCollection fromModel(@Nullable ArtworkCollectionResource model) {
      if (model == null) return null;
      final CollectionModel<ArtworkResource> collection = model.getCollection();
      if (collection == null) return null;
      final List<Artwork> artwork =
          collection.getContent().stream().map(artworkModeller::fromModel).toList();
      final ArtworkCollection artworkCollection = new ArtworkCollection(model.getRole());
      artworkCollection.setId(model.getId());
      artworkCollection.addAll(artwork);
      artworkCollection.setSelectedIndex(model.getSelectedIndex());
      return artworkCollection;
    }
  }
}
