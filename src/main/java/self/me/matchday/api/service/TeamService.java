package self.me.matchday.api.service;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Service;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

@Service
public class TeamService {

  private static final String LOG_TAG = "TeamService";

  private final TeamRepository teamRepository;
  private final TeamResourceAssembler teamResourceAssembler;

  @Autowired
  public TeamService(TeamRepository teamRepository, TeamResourceAssembler teamResourceAssembler) {

    this.teamRepository = teamRepository;
    this.teamResourceAssembler = teamResourceAssembler;
  }

  /**
   * Fetch all teams from the local database.
   *
   * @return Optional containing a collection model of Team resources.
   */
  public Optional<CollectionModel<TeamResource>> fetchAllTeams() {

    Log.i(LOG_TAG, "Fetching all Teams from local database.");

    final List<Team> teams = teamRepository.findAll();
    if (teams.size() > 0) {
      return Optional.of(teamResourceAssembler.toCollectionModel(teams));
    } else {
      Log.d(LOG_TAG, "Attempted to fetch all Teams, but nothing found.");
      return Optional.empty();
    }
  }

  /**
   * Fetch a single Team from the database, given an ID.
   *
   * @param teamId The Team ID.
   * @return The requested Team, wrapped in an Optional.
   */
  public Optional<TeamResource> fetchTeamById(@NotNull final String teamId) {

    Log.i(LOG_TAG, String.format("Fetching Team with ID: %s from the local database.", teamId));
    return
        teamRepository
            .findById(teamId)
            .map(teamResourceAssembler::toModel);
  }
}
