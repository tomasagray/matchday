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
import static self.me.matchday.util.Constants.*;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.TeamController;
import self.me.matchday.api.resource.ArtworkCollectionResource.ArtworkCollectionResourceAssembler;
import self.me.matchday.api.resource.ColorResource.ColorResourceModeller;
import self.me.matchday.model.ArtworkRole;
import self.me.matchday.model.Country;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Team;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "team")
@Relation(collectionRelation = "teams")
public class TeamResource extends RepresentationModel<TeamResource> {

  private UUID id;
  private ProperName name;
  private Country country;
  private List<ColorResource> colors;
  private ArtworkCollectionResource emblem;
  private ArtworkCollectionResource fanart;

  @Component
  public static class TeamResourceAssembler
      extends RepresentationModelAssemblerSupport<Team, TeamResource> {

    private final ArtworkCollectionResourceAssembler collectionModeller;
    private final ColorResourceModeller colorModeller;

    public TeamResourceAssembler(
        ArtworkCollectionResourceAssembler collectionModeller,
        ColorResourceModeller colorModeller) {
      super(TeamController.class, TeamResource.class);
      this.collectionModeller = collectionModeller;
      this.colorModeller = colorModeller;
    }

    public static void addArtworkLinks(
        @NotNull UUID teamId, @NotNull ArtworkRole role, @NotNull ArtworkResource resource) {
      try {
        final Long artworkId = resource.getId();
        resource.add(
            linkTo(methodOn(TeamController.class).fetchTeamArtworkMetadata(teamId, role, artworkId))
                .withRel("metadata"));
        resource.add(
            linkTo(
                    methodOn(TeamController.class)
                        .fetchTeamArtworkImageData(teamId, role, artworkId))
                .withRel("image"));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @SneakyThrows
    @NotNull
    @Override
    public TeamResource toModel(@NotNull Team team) {

      // initialize resource
      final TeamResource teamResource = instantiateModel(team);
      final UUID teamId = team.getId();
      teamResource.setId(teamId);
      teamResource.setName(team.getName());
      teamResource.setCountry(team.getCountry());
      teamResource.setColors(getColorResources(team));

      // artwork
      teamResource.setEmblem(collectionModeller.toModel(team.getEmblem()));
      teamResource.setFanart(collectionModeller.toModel(team.getFanart()));
      teamResource
          .getEmblem()
          .getArtwork()
          .forEach(artwork -> addArtworkLinks(teamId, ArtworkRole.EMBLEM, artwork));
      teamResource
          .getFanart()
          .getArtwork()
          .forEach(artwork -> addArtworkLinks(teamId, ArtworkRole.FANART, artwork));

      // attach links
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchTeamByName(teamId)).withSelfRel());
      // artwork
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchSelectedArtwork(teamId, ArtworkRole.EMBLEM))
              .withRel(EMBLEM_REL));
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchSelectedArtwork(teamId, ArtworkRole.FANART))
              .withRel(FANART_REL));
      // events
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchEventsForTeam(teamId)).withRel(EVENTS_REL));

      return teamResource;
    }

    private List<ColorResource> getColorResources(@NotNull Team team) {
      return team.getColors().stream().map(colorModeller::toModel).collect(Collectors.toList());
    }
  }
}
