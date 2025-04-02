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

import static net.tomasbot.matchday.api.resource.EventsResource.VIDEO_LINK;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.matchday.api.controller.EventController;
import net.tomasbot.matchday.api.controller.MatchController;
import net.tomasbot.matchday.api.resource.CompetitionResource.CompetitionModeller;
import net.tomasbot.matchday.api.resource.TeamResource.TeamModeller;
import net.tomasbot.matchday.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "match")
@Relation(collectionRelation = "matches")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class MatchResource extends RepresentationModel<MatchResource> {

  private static final String ARTWORK_REL = "artwork";

  private UUID eventId;
  private String title;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;
  private CompetitionResource competition;
  private TeamResource homeTeam;
  private TeamResource awayTeam;

  @Component
  public static class MatchResourceAssembler extends EntityModeller<Match, MatchResource> {

    private final CompetitionModeller competitionAssembler;
    private final TeamModeller teamAssembler;

    public MatchResourceAssembler(
        CompetitionModeller competitionAssembler, TeamModeller teamAssembler) {
      super(MatchController.class, MatchResource.class);
      this.competitionAssembler = competitionAssembler;
      this.teamAssembler = teamAssembler;
    }

    @Override
    public @NotNull MatchResource toModel(@NotNull Match entity) {
      try {
        final MatchResource resource = instantiateModel(entity);
        final CompetitionResource competition =
            competitionAssembler.toModel(entity.getCompetition());
        final TeamResource homeTeam = teamAssembler.toModel(entity.getHomeTeam());
        final TeamResource awayTeam = teamAssembler.toModel(entity.getAwayTeam());

        final UUID eventId = entity.getEventId();
        resource.setEventId(eventId);
        resource.setTitle(entity.getTitle());
        resource.setSeason(entity.getSeason());
        resource.setFixture(entity.getFixture());
        resource.setDate(entity.getDate());
        resource.setCompetition(competition);
        resource.setHomeTeam(homeTeam);
        resource.setAwayTeam(awayTeam);

        resource.add(linkTo(methodOn(MatchController.class).fetchMatchById(eventId)).withSelfRel());
        Artwork artwork = entity.getArtwork();
        if (artwork != null) {
          resource.add(
              linkTo(
                      methodOn(MatchController.class)
                          .fetchMatchArtworkImage(eventId, artwork.getId()))
                  .withRel(ARTWORK_REL));
        }
        resource.add(
            linkTo(methodOn(EventController.class).getVideoResources(eventId)).withRel(VIDEO_LINK));

        return resource;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public Match fromModel(@Nullable MatchResource resource) {
      if (resource == null) return null;
      final Competition competition = competitionAssembler.fromModel(resource.getCompetition());
      final Team homeTeam = teamAssembler.fromModel(resource.getHomeTeam());
      final Team awayTeam = teamAssembler.fromModel(resource.getAwayTeam());
      return Match.builder()
          .eventId(resource.getEventId())
          .competition(competition)
          .homeTeam(homeTeam)
          .awayTeam(awayTeam)
          .date(resource.getDate())
          .season(resource.getSeason())
          .fixture(resource.getFixture())
          .build();
    }
  }
}
