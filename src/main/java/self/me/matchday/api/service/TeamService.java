package self.me.matchday.api.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
      // Sort Teams by name
      teams.sort(Comparator.comparing(Team::getName));
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

    Log.i(LOG_TAG, String.format("Fetching Team with ID: %s from local database.", teamId));
    return
        teamRepository
            .findById(teamId)
            .map(teamResourceAssembler::toModel);
  }

  /**
   * Retrieve all Teams for a given Competition, specified by the competitionId.
   *
   * @param competitionId The ID of the Competition.
   * @return All Teams which have Events in the given Competition.
   */
  public Optional<CollectionModel<TeamResource>> fetchTeamsByCompetitionId(
      @NotNull final String competitionId) {

    Log.i(LOG_TAG,
        String.format("Fetching all Teams for Competition ID: %s from local database.", competitionId));

    // Get home teams
    final List<Team> homeTeams = teamRepository.fetchHomeTeamsByCompetition(competitionId);
    // Get away teams
    final List<Team> awayTeams = teamRepository.fetchAwayTeamsByCompetition(competitionId);
    // Combine results in a Set<> to ensure no duplicates
    Set<Team> teamSet = new LinkedHashSet<>(homeTeams);
    teamSet.addAll(awayTeams);
    // Convert back to a List<> for sorting
    List<Team> teamList = new ArrayList<>(teamSet);
    // Sort by Team name
    teamList.sort(Comparator.comparing(Team::getName));
    return Optional.of(teamResourceAssembler.toCollectionModel(teamList));
  }
}
