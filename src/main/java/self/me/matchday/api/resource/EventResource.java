package self.me.matchday.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
import self.me.matchday.model.Event;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;

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
  public static class EventResourceAssembler extends
      RepresentationModelAssemblerSupport<Event, EventResource> {

    private final static LinkRelation VIDEO_LINK = LinkRelation.of("video");

    private final TeamResourceAssembler teamResourceAssembler;
    private final CompetitionResourceAssembler competitionResourceAssembler;

    @Autowired
    public EventResourceAssembler(TeamResourceAssembler teamResourceAssembler,
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
          linkTo(methodOn(VideoStreamingController.class)
              .getVideoResources(entity.getEventId()))
              .withRel(VIDEO_LINK));

      // Handle subtypes
      if (entity instanceof Match) {
        // Cast to Match
        final Match match = (Match) entity;
        eventResource.setHomeTeam(teamResourceAssembler.toModel(match.getHomeTeam()));
        eventResource.setAwayTeam(teamResourceAssembler.toModel(match.getAwayTeam()));
        // add self link
        eventResource.add(linkTo(
            methodOn(MatchController.class)
                .fetchMatchById(match.getEventId()))
            .withSelfRel());
      } else {
        // it's a Highlight; add self link
        eventResource.add(linkTo(
            methodOn(HighlightController.class)
                .fetchHighlightById(entity.getEventId()))
            .withSelfRel());
      }
      // Return the finished product
      return eventResource;
    }

    @Override
    public @NotNull CollectionModel<EventResource> toCollectionModel(
        @NotNull Iterable<? extends Event> entities) {

      // Instantiate from super method
      final CollectionModel<EventResource> eventResources = super.toCollectionModel(entities);
      // Add self link
      eventResources
          .add(linkTo(methodOn(EventController.class)
              .fetchAllEvents())
              .withSelfRel());

      return eventResources;
    }
  }
}
