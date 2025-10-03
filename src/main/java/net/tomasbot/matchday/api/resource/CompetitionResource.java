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

import static net.tomasbot.matchday.util.Constants.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.tomasbot.matchday.api.controller.CompetitionController;
import net.tomasbot.matchday.api.controller.RootController;
import net.tomasbot.matchday.api.resource.ArtworkCollectionResource.ArtworkCollectionModeller;
import net.tomasbot.matchday.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "competition")
@Relation(collectionRelation = "competitions")
public class CompetitionResource extends RepresentationModel<CompetitionResource> {

  private UUID id;
  private ProperName name;
  private Country country;
  private ArtworkCollectionResource emblem;
  private ArtworkCollectionResource fanart;

  @Component
  public static class CompetitionModeller extends EntityModeller<Competition, CompetitionResource> {
    
    private final ArtworkCollectionModeller artworkModeller;

    public CompetitionModeller(ArtworkCollectionModeller artworkModeller) {
      super(CompetitionController.class, CompetitionResource.class);
      this.artworkModeller = artworkModeller;
    }

    public static void addArtworkLinks(
        @NotNull UUID competitionId,
        @NotNull ArtworkRole role,
        @NotNull ArtworkResource artworkResource) {
      try {
        final Long artworkId = artworkResource.getId();
        artworkResource.add(
            linkTo(
                    methodOn(CompetitionController.class)
                        .fetchArtworkMetadata(competitionId, role, artworkId))
                .withRel(LinkRelations.METADATA_REL));
        artworkResource.add(
            linkTo(
                    methodOn(CompetitionController.class)
                        .fetchArtworkImageData(competitionId, role, artworkId))
                .withRel(LinkRelations.IMAGE_REL));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @SneakyThrows
    @NotNull
    @Override
    @Transactional
    public CompetitionResource toModel(@NotNull Competition competition) {
      final CompetitionResource competitionResource = instantiateModel(competition);

      final UUID competitionId = competition.getId();
      competitionResource.setId(competitionId);
      competitionResource.setCountry(competition.getCountry());
      competitionResource.setName(competition.getName());

      // setup artwork
      competitionResource.setEmblem(artworkModeller.toModel(competition.getEmblem()));
      competitionResource.setFanart(artworkModeller.toModel(competition.getFanart()));
      competitionResource
          .getFanart()
          .getCollection()
          .forEach(artwork -> addArtworkLinks(competitionId, ArtworkRole.FANART, artwork));
      competitionResource
          .getEmblem()
          .getCollection()
          .forEach(artwork -> addArtworkLinks(competitionId, ArtworkRole.EMBLEM, artwork));

      // links
      competitionResource.add(
          linkTo(methodOn(CompetitionController.class).fetchCompetitionById(competitionId))
              .withSelfRel());
      competitionResource.add(
          linkTo(
                  methodOn(CompetitionController.class)
                      .fetchCompetitionEvents(
                          competitionId,
                          RootController.DEFAULT_PAGE,
                          RootController.DEFAULT_PAGE_SIZE))
              .withRel(LinkRelations.EVENTS_REL));
      competitionResource.add(
          linkTo(methodOn(CompetitionController.class).fetchCompetitionTeams(competitionId))
              .withRel(LinkRelations.TEAMS_REL));
      // artwork collection links
      competitionResource.add(
          linkTo(
                  methodOn(CompetitionController.class)
                      .fetchSelectedArtwork(competitionId, ArtworkRole.EMBLEM))
              .withRel(LinkRelations.EMBLEM_REL));
      competitionResource.add(
          linkTo(
                  methodOn(CompetitionController.class)
                      .fetchSelectedArtwork(competitionId, ArtworkRole.FANART))
              .withRel(LinkRelations.FANART_REL));
      return competitionResource;
    }

    @NotNull
    @Override
    public CollectionModel<CompetitionResource> toCollectionModel(
        @NotNull Iterable<? extends Competition> competitions) {

      final CollectionModel<CompetitionResource> competitionResources =
          super.toCollectionModel(competitions);
      // add a self link
      competitionResources.add(
          linkTo(methodOn(CompetitionController.class).fetchAllCompetitions()).withSelfRel());
      return competitionResources;
    }

    @Override
    public Competition fromModel(@Nullable CompetitionResource resource) {
      if (resource == null) return null;
      final ArtworkCollection emblem =
          getArtworkCollection(resource.getEmblem(), ArtworkRole.EMBLEM);
      final ArtworkCollection fanart =
          getArtworkCollection(resource.getFanart(), ArtworkRole.FANART);
      final Competition competition = new Competition(resource.getName());
      competition.setId(resource.getId());
      competition.setCountry(resource.getCountry());
      competition.setEmblem(emblem);
      competition.setFanart(fanart);
      return competition;
    }

    private ArtworkCollection getArtworkCollection(
        @NotNull ArtworkCollectionResource resource, @NotNull ArtworkRole role) {
      final ArtworkCollection collection = artworkModeller.fromModel(resource);
      if (collection != null) collection.setRole(role);
      return collection;
    }
  }
}
