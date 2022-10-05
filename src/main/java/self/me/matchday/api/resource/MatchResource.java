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
import static self.me.matchday.api.resource.EventsResource.VIDEO_LINK;

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
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.MatchController;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;

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
  private RepresentationModel<CompetitionResource> competition;
  private RepresentationModel<TeamResource> homeTeam;
  private RepresentationModel<TeamResource> awayTeam;

  @Component
  public static class MatchResourceAssembler
      extends RepresentationModelAssemblerSupport<Match, MatchResource> {

    private final CompetitionResourceAssembler competitionAssembler;
    private final TeamResourceAssembler teamAssembler;

    public MatchResourceAssembler(
        CompetitionResourceAssembler competitionAssembler, TeamResourceAssembler teamAssembler) {
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
        resource.add(
            linkTo(methodOn(MatchController.class).fetchMatchArtworkImage(eventId))
                .withRel(ARTWORK_REL));
        resource.add(
            linkTo(methodOn(VideoStreamingController.class).getVideoResources(eventId))
                .withRel(VIDEO_LINK));
        resource.add(linkTo(methodOn(MatchController.class).fetchMatchById(eventId)).withSelfRel());
        return resource;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
