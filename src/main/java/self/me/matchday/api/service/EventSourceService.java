package self.me.matchday.api.service;

import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.db.EventSourceRepository;
import self.me.matchday.db.FixtureRepository;
import self.me.matchday.db.HighlightShowRepository;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.db.SeasonRepository;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventSource;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

@Service
public class EventSourceService {

  private static final String LOG_TAG = "EventSourceService";

  // Repos
  private final EventSourceRepository eventSourceRepository;
  private final CompetitionRepository competitionRepository;
  private final TeamRepository teamRepository;
  private final SeasonRepository seasonRepository;
  private final FixtureRepository fixtureRepository;
  private final MatchRepository matchRepository;
  private final HighlightShowRepository highlightShowRepository;

  @Autowired
  public EventSourceService(EventSourceRepository eventSourceRepository,
      CompetitionRepository competitionRepository, TeamRepository teamRepository,
      SeasonRepository seasonRepository, FixtureRepository fixtureRepository,
      MatchRepository matchRepository, HighlightShowRepository highlightShowRepository) {

    this.eventSourceRepository = eventSourceRepository;
    this.competitionRepository = competitionRepository;
    this.teamRepository = teamRepository;
    this.seasonRepository = seasonRepository;
    this.fixtureRepository = fixtureRepository;
    this.matchRepository = matchRepository;
    this.highlightShowRepository = highlightShowRepository;
  }

  @Transactional
  public void save(@NotNull final EventSource eventSource) {

    Log.i(LOG_TAG, String.format("Saving EventSource components for EventSource: %s",
        eventSource.getEventSourceId()));

    // Only save if this EventSource has playable files
    if (countFileSources(eventSource) > 0) {
      // Get the Event
      final Event event = eventSource.getEvent();
      // Save common components
      Log.i(LOG_TAG, "Saving Competition: " + competitionRepository.save(event.getCompetition()));
      Log.i(LOG_TAG, "Saving Season: " + seasonRepository.save(event.getSeason()));
      Log.i(LOG_TAG, "Saving Fixture: " + fixtureRepository.save(event.getFixture()));

      // Handle Match data
      if (event instanceof Match) {
        // Cast to Match
        Match match = (Match)event;
        // Save Match
        Log.i(LOG_TAG, "Saving Home Team: " + teamRepository.save(match.getHomeTeam()));
        Log.i(LOG_TAG, "Saving Away Team: " + teamRepository.save(match.getAwayTeam()));
        Log.i(LOG_TAG, "Saving Match: " + matchRepository.save(match));
      } else {
        // Handle Highlight Show
        final HighlightShow highlightShow = (HighlightShow) event;
        Log.i(LOG_TAG, "Saving Highlight Show: " + highlightShowRepository.save(highlightShow));
      }
      // Save the EventSource
      Log.i(LOG_TAG, "Saving EventSource: " + eventSourceRepository.save(eventSource));

    } else {
      Log.i(LOG_TAG, String.format("Did not save EventSource: %s; no EventFiles", eventSource));
    }
  }

  /**
   * Count the number of EventFiles for each EventFileSource in the given EventFile.
   *
   * @param eventSource The EventSource.
   * @return Number of EventFiles in this source
   */
  private int countFileSources(@NotNull final EventSource eventSource) {

    // Result container
    AtomicInteger count = new AtomicInteger();
    // Count EventFiles
    eventSource
        .getEventFileSources()
        .forEach(eventFileSource -> count.addAndGet(eventFileSource.getEventFiles().size()));
    // Return total
    return count.get();
  }
}
