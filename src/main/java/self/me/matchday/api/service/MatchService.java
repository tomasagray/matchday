package self.me.matchday.api.service;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

@Service
public class MatchService {

  private static final String LOG_TAG = "MatchController";
  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final MatchRepository matchRepository;

  @Autowired
  public MatchService(final MatchRepository matchRepository) {

    this.matchRepository = matchRepository;
  }

  /**
   * Retrieve all Matches from the repo (database) and assemble into a collection of resources.
   *
   * @return Collection of assembled resources.
   */
  public Optional<List<Match>> fetchAllMatches() {

    Log.i(LOG_TAG, "Fetching all Matches from database.");
    // Retrieve all matches from repo
    final List<Match> matches = matchRepository.findAll();

    if (matches.size() > 0) {
      // Sort by date (descending) & return
      matches.sort(EVENT_SORTER);
      return Optional.of(matches);
    } else {
      Log.d(LOG_TAG, "Attempting to retrieve all Matches, but none found");
      return Optional.empty();
    }
  }

  /**
   * Retrieve a specific match from the local DB.
   *
   * @param matchId The ID of the match we want.
   * @return An optional containing the match resource, if it was found.
   */
  public Optional<Match> fetchMatch(@NotNull String matchId) {

    Log.i(LOG_TAG, String.format("Fetching Match with ID: %s from the database.", matchId));
    return
        matchRepository
            .findById(matchId);
  }
}