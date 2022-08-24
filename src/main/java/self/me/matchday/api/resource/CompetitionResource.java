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

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.ArtworkController;
import self.me.matchday.api.controller.CompetitionController;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Country;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "competition")
@Relation(collectionRelation = "competitions")
public class CompetitionResource extends RepresentationModel<CompetitionResource> {

  private UUID id;
  private String name;
  private List<Synonym> synonyms;
  private Country country;

  @Component
  public static class CompetitionResourceAssembler
      extends RepresentationModelAssemblerSupport<Competition, CompetitionResource> {

    private static final LinkRelation TEAMS = LinkRelation.of("teams");
    private static final LinkRelation EMBLEM = LinkRelation.of("emblem");
    private static final LinkRelation FANART = LinkRelation.of("fanart");
    private static final LinkRelation MONOCHROME = LinkRelation.of("monochrome_emblem");
    private static final LinkRelation LANDSCAPE = LinkRelation.of("landscape");
    private static final LinkRelation EVENTS = LinkRelation.of("events");

    public CompetitionResourceAssembler() {
      super(CompetitionController.class, CompetitionResource.class);
    }

    @SneakyThrows
    @NotNull
    @Override
    public CompetitionResource toModel(@NotNull Competition competition) {

      final CompetitionResource competitionResource = instantiateModel(competition);

      final UUID competitionId = competition.getCompetitionId();
      final ProperName properName = competition.getName();
      competitionResource.setId(competitionId);
      competitionResource.setName(properName.getName());
      competitionResource.setSynonyms(properName.getSynonyms());
      competitionResource.setCountry(competition.getCountry());

      competitionResource.add(
          linkTo(methodOn(CompetitionController.class).fetchCompetitionById(competitionId))
              .withSelfRel());
      competitionResource.add(
          linkTo(methodOn(CompetitionController.class).fetchCompetitionEvents(competitionId))
              .withRel(EVENTS));
      competitionResource.add(
          linkTo(methodOn(CompetitionController.class).fetchCompetitionTeams(competitionId))
              .withRel(TEAMS));
      competitionResource.add(
          linkTo(methodOn(ArtworkController.class).fetchCompetitionEmblem(competitionId))
              .withRel(EMBLEM));
      competitionResource.add(
          linkTo(methodOn(ArtworkController.class).fetchCompetitionFanart(competitionId))
              .withRel(FANART));
      competitionResource.add(
          linkTo(methodOn(ArtworkController.class).fetchCompetitionMonochromeEmblem(competitionId))
              .withRel(MONOCHROME));
      competitionResource.add(
          linkTo(methodOn(ArtworkController.class).fetchCompetitionLandscape(competitionId))
              .withRel(LANDSCAPE));

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
  }
}
