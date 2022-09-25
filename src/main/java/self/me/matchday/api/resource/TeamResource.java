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
import self.me.matchday.api.controller.TeamController;
import self.me.matchday.model.Country;
import self.me.matchday.model.Team;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "team")
@Relation(collectionRelation = "teams")
public class TeamResource extends RepresentationModel<TeamResource> {

  private UUID id;
  private String name;
  private Country country;

  @Component
  public static class TeamResourceAssembler
      extends RepresentationModelAssemblerSupport<Team, TeamResource> {

    private static final LinkRelation EVENTS = LinkRelation.of("events");
    private static final LinkRelation EMBLEM = LinkRelation.of("emblem");
    private static final LinkRelation FANART = LinkRelation.of("fanart");

    public TeamResourceAssembler() {
      super(TeamController.class, TeamResource.class);
    }

    @SneakyThrows
    @NotNull
    @Override
    public TeamResource toModel(@NotNull Team team) {

      final TeamResource teamResource = instantiateModel(team);
      // initialize resource
      final UUID teamId = team.getTeamId();
      teamResource.setId(teamId);
      teamResource.setName(team.getName().getName());
      teamResource.setCountry(team.getCountry());
      // attach links
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchTeamByName(teamId)).withSelfRel());
      // artwork
      /*      teamResource.add(
          linkTo(methodOn(ArtworkController.class).fetchTeamEmblem(teamId)).withRel(EMBLEM));
      teamResource.add(
          linkTo(methodOn(ArtworkController.class).fetchTeamFanart(teamId)).withRel(FANART));*/
      // events
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchEventsForTeam(teamId)).withRel(EVENTS));

      return teamResource;
    }

    @NotNull
    @Override
    public CollectionModel<TeamResource> toCollectionModel(
        @NotNull Iterable<? extends Team> teams) {

      final CollectionModel<TeamResource> teamResources = super.toCollectionModel(teams);
      // add a self link
      teamResources.add(linkTo(methodOn(TeamController.class).fetchAllTeams()).withSelfRel());
      return teamResources;
    }
  }
}
