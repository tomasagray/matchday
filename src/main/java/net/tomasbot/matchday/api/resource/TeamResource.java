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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import net.tomasbot.matchday.api.controller.TeamController;
import net.tomasbot.matchday.api.resource.ArtworkCollectionResource.ArtworkCollectionModeller;
import net.tomasbot.matchday.api.resource.ColorResource.ColorResourceModeller;
import net.tomasbot.matchday.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;

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
  public static class TeamModeller extends EntityModeller<Team, TeamResource> {

    private final ArtworkCollectionModeller artworkModeller;
    private final ColorResourceModeller colorModeller;

    public TeamModeller(
        ArtworkCollectionModeller artworkModeller, ColorResourceModeller colorModeller) {
      super(TeamController.class, TeamResource.class);
      this.artworkModeller = artworkModeller;
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
      teamResource.setEmblem(artworkModeller.toModel(team.getEmblem()));
      teamResource.setFanart(artworkModeller.toModel(team.getFanart()));
      teamResource
          .getEmblem()
          .getCollection()
          .forEach(artwork -> addArtworkLinks(teamId, ArtworkRole.EMBLEM, artwork));
      teamResource
          .getFanart()
          .getCollection()
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

    @Override
    public Team fromModel(@Nullable TeamResource resource) {
      if (resource == null) return null;
      final ArtworkCollection emblem =
          getArtworkCollection(resource.getEmblem(), ArtworkRole.EMBLEM);
      final ArtworkCollection fanart =
          getArtworkCollection(resource.getFanart(), ArtworkRole.FANART);
      final Team team = new Team(resource.getName());
      team.setId(resource.getId());
      team.setColors(getColorsFromResources(resource.getColors()));
      team.setCountry(resource.getCountry());
      team.setEmblem(emblem);
      team.setFanart(fanart);
      return team;
    }

    private ArtworkCollection getArtworkCollection(
        @NotNull ArtworkCollectionResource resource, @NotNull ArtworkRole role) {
      final ArtworkCollection collection = artworkModeller.fromModel(resource);
      if (collection != null) {
        collection.setRole(role);
      }
      return collection;
    }

    private List<ColorResource> getColorResources(@NotNull Team team) {
      List<Color> colors = team.getColors();
      if (colors == null) return List.of();
      return colors.stream().map(colorModeller::toModel).collect(Collectors.toList());
    }

    private List<Color> getColorsFromResources(@Nullable List<ColorResource> resources) {
      if (resources == null) return List.of();
      return resources.stream().map(colorModeller::fromModel).toList();
    }
  }
}
