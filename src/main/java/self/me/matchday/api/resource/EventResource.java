/*
 * Copyright (c) 2021.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.controller.HighlightController;
import self.me.matchday.api.controller.MatchController;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "event")
@Relation(collectionRelation = "events")
@JsonInclude(value = Include.NON_NULL)
public class EventResource extends RepresentationModel<EventResource> {

  // TODO: Add MatchResource & HighlightResource

  private String eventId;
  private String title;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;
  private RepresentationModel<CompetitionResource> competition;
  // Only for Matches
  private RepresentationModel<TeamResource> homeTeam;
  private RepresentationModel<TeamResource> awayTeam;

  @Component
  public static class EventResourceAssembler
      extends RepresentationModelAssemblerSupport<Event, EventResource> {

    private static final LinkRelation VIDEO_LINK = LinkRelation.of("video");

    private final TeamResourceAssembler teamResourceAssembler;
    private final CompetitionResourceAssembler competitionResourceAssembler;

    @Autowired
    public EventResourceAssembler(
        TeamResourceAssembler teamResourceAssembler,
        CompetitionResourceAssembler competitionResourceAssembler) {

      super(EventController.class, EventResource.class);
      this.teamResourceAssembler = teamResourceAssembler;
      this.competitionResourceAssembler = competitionResourceAssembler;
    }

    @NotNull
    @Override
    public EventResource toModel(@NotNull Event entity) {

      // Instantiate model
      final EventResource eventResource = instantiateModel(entity);

      // Create Competition resource so links are included
      final CompetitionResource competitionResource =
          competitionResourceAssembler.toModel(entity.getCompetition());
      // populate DTO
      eventResource.setEventId(entity.getEventId());
      eventResource.setTitle(entity.getTitle());
      eventResource.setCompetition(competitionResource);
      eventResource.setSeason(entity.getSeason());
      eventResource.setFixture(entity.getFixture());
      eventResource.setDate(entity.getDate());

      // TODO: Make this handle correct subtype
      // Add link to playlist resource for this event
      eventResource.add(
          linkTo(methodOn(VideoStreamingController.class).getVideoResources(entity.getEventId()))
              .withRel(VIDEO_LINK));

      // Handle subtypes
      if (entity instanceof Match) {
        // Cast to Match
        final Match match = (Match) entity;
        final Team homeTeam = match.getHomeTeam();
        final Team awayTeam = match.getAwayTeam();
        if (homeTeam != null) {
          eventResource.setHomeTeam(teamResourceAssembler.toModel(homeTeam));
        }
        if (awayTeam != null) {
          eventResource.setAwayTeam(teamResourceAssembler.toModel(awayTeam));
        }
        eventResource.add(
            linkTo(methodOn(MatchController.class).fetchMatchById(match.getEventId()))
                .withSelfRel());
      } else {
        eventResource.add(
            linkTo(methodOn(HighlightController.class).fetchHighlightById(entity.getEventId()))
                .withSelfRel());
      }
      return eventResource;
    }

    @Override
    public @NotNull CollectionModel<EventResource> toCollectionModel(
        @NotNull Iterable<? extends Event> entities) {

      // Sort Events
      final List<? extends Event> sortedEvents =
          StreamSupport.stream(entities.spliterator(), false)
              .sorted(new Event.EventSorter())
              .collect(Collectors.toList());
      // Instantiate from super method
      final CollectionModel<EventResource> eventResources = super.toCollectionModel(sortedEvents);
      // Add self link
      eventResources.add(linkTo(methodOn(EventController.class).fetchAllEvents()).withSelfRel());

      return eventResources;
    }
  }
}
