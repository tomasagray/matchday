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
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.PlaylistResource.PlaylistResourceAssembler;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.MasterPlaylistService;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Match;
import self.me.matchday.model.Season;

/**
 * Represents the API view of a Match. Attaches and hides related data as necessary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "match")
@Relation(collectionRelation = "matches")
@JsonInclude(value = Include.NON_NULL)
public class MatchResource extends RepresentationModel<MatchResource> {

  private String id;
  private RepresentationModel<TeamResource> homeTeam;
  private RepresentationModel<TeamResource> awayTeam;
  private RepresentationModel<CompetitionResource> competition;
  private RepresentationModel<PlaylistResource> playlists;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;

  @Component
  public static class MatchResourceAssembler extends
      RepresentationModelAssemblerSupport<Match, MatchResource> {

    private final MasterPlaylistService masterPlaylistService;
    private final TeamResourceAssembler teamResourceAssembler;
    private final CompetitionResourceAssembler competitionResourceAssembler;
    private final PlaylistResourceAssembler playlistResourceAssembler;

    @Autowired
    public MatchResourceAssembler(MasterPlaylistService masterPlaylistService,
        TeamResourceAssembler teamResourceAssembler,
        CompetitionResourceAssembler competitionResourceAssembler,
        PlaylistResourceAssembler playlistResourceAssembler) {

      super(EventController.class, MatchResource.class);
      this.masterPlaylistService = masterPlaylistService;
      this.teamResourceAssembler = teamResourceAssembler;
      this.competitionResourceAssembler = competitionResourceAssembler;
      this.playlistResourceAssembler = playlistResourceAssembler;
    }

    @NotNull
    @Override
    public MatchResource toModel(@NotNull final Match match) {

      // Instantiate model
      final MatchResource matchResource = instantiateModel(match);

      // populate DTO
      matchResource.setId(match.getEventId());
      matchResource.setHomeTeam(teamResourceAssembler.toModel(match.getHomeTeam()));
      matchResource.setAwayTeam(teamResourceAssembler.toModel(match.getAwayTeam()));
      matchResource.setCompetition(competitionResourceAssembler.toModel(match.getCompetition()));
      matchResource.setSeason(match.getSeason());
      matchResource.setFixture(match.getFixture());
      matchResource.setDate(match.getDate());
      // Set Master Playlist for this Match
      masterPlaylistService
          .fetchPlaylistById(match.getEventId())
          .ifPresent(
              masterM3U -> matchResource.setPlaylists(playlistResourceAssembler.toModel(masterM3U))
          );
      // attach self link
      matchResource.add(
          linkTo(methodOn(EventController.class).fetchMatchById(match.getEventId()))
              .withSelfRel());

      return matchResource;
    }

    @NotNull
    @Override
    public CollectionModel<MatchResource> toCollectionModel(
        @NotNull Iterable<? extends Match> matches) {

      final CollectionModel<MatchResource> matchResources = super.toCollectionModel(matches);
      // add a self link to collection
      matchResources.add(linkTo(methodOn(EventController.class).fetchAllMatches()).withSelfRel());
      return matchResources;
    }
  }
}
