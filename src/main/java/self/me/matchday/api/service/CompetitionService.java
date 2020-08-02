package self.me.matchday.api.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.util.Log;

@Service
public class CompetitionService {

  private static final String LOG_TAG = "CompetitionService";

  private final CompetitionRepository competitionRepository;

  @Autowired
  public CompetitionService(final CompetitionRepository competitionRepository) {
    this.competitionRepository = competitionRepository;
  }

  /**
   * Fetch all Competitions in the database.
   *
   * @return A CollectionModel of Competition resources.
   */
  public Optional<List<Competition>> fetchAllCompetitions() {

    Log.i(LOG_TAG, "Retrieving all Competitions from database.");

    final List<Competition> competitions = competitionRepository.findAll();
    if (competitions.size() > 0) {
      // Sort Competitions by name
      competitions.sort(Comparator.comparing(Competition::getName));
      return Optional.of(competitions);
    } else {
      Log.i(LOG_TAG, "Attempted to fetch all Competitions, but none returned");
      return Optional.empty();
    }
  }

  /**
   * Fetch a specific Competition from the database.
   *
   * @param competitionId The ID of the desired Competition.
   * @return The Competition as a resource.
   */
  public Optional<Competition> fetchCompetitionById(@NotNull String competitionId) {

    Log.i(LOG_TAG,
        String.format("Fetching competition with ID: %s from the database.", competitionId));
    return
        competitionRepository
            .findById(competitionId);
  }
}
