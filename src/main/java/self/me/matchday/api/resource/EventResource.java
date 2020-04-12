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
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.PlaylistResource.PlaylistResourceAssembler;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.MasterPlaylistService;
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

  private String eventId;
  private String title;
  // Only for Matches
  private RepresentationModel<TeamResource> homeTeam;
  private RepresentationModel<TeamResource> awayTeam;
  private RepresentationModel<CompetitionResource> competition;
  private RepresentationModel<PlaylistResource> playlists;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;

  @Component
  public static class EventResourceAssembler extends
      RepresentationModelAssemblerSupport<Event, EventResource> {

    private final MasterPlaylistService masterPlaylistService;
    private final PlaylistResourceAssembler playlistResourceAssembler;
    private final TeamResourceAssembler teamResourceAssembler;
    private final CompetitionResourceAssembler competitionResourceAssembler;

    @Autowired
    public EventResourceAssembler(MasterPlaylistService masterPlaylistService,
        PlaylistResourceAssembler playlistResourceAssembler,
        TeamResourceAssembler teamResourceAssembler,
        CompetitionResourceAssembler competitionResourceAssembler) {

      super(EventController.class, EventResource.class);
      this.masterPlaylistService = masterPlaylistService;
      this.playlistResourceAssembler = playlistResourceAssembler;
      this.teamResourceAssembler = teamResourceAssembler;
      this.competitionResourceAssembler = competitionResourceAssembler;
    }

    @NotNull
    @Override
    public EventResource toModel(@NotNull Event entity) {

      // Instantiate model
      final EventResource eventResource = instantiateModel(entity);

      // populate DTO
      eventResource.setEventId(entity.getEventId());
      eventResource.setTitle(entity.getTitle());
      if (entity instanceof Match) {
        // Cast to Match
        final Match match = (Match) entity;
        eventResource.setHomeTeam(teamResourceAssembler.toModel(match.getHomeTeam()));
        eventResource.setAwayTeam(teamResourceAssembler.toModel(match.getAwayTeam()));
        // add self link
        eventResource.add(linkTo(methodOn(EventController.class).fetchMatchById(match.getEventId()))
            .withSelfRel());
      } else {
        // it's a HighlightShow; add appropriate link
        eventResource.add(
            linkTo(methodOn(EventController.class).fetchHighlightById(entity.getEventId()))
                .withSelfRel());
      }

      eventResource.setCompetition(competitionResourceAssembler.toModel(entity.getCompetition()));
      eventResource.setSeason(entity.getSeason());
      eventResource.setFixture(entity.getFixture());
      eventResource.setDate(entity.getDate());
      // Set Master Playlist for this Event
      masterPlaylistService
          .fetchPlaylistById(entity.getEventId())
          .ifPresent(
              masterM3U -> eventResource.setPlaylists(playlistResourceAssembler.toModel(masterM3U))
          );

      return eventResource;
    }
  }
}
