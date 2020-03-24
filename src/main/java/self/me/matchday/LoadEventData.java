/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday;

import java.net.URL;
import javax.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.db.EventSourceRepository;
import self.me.matchday.db.FixtureRepository;
import self.me.matchday.db.HighlightShowRepository;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.db.SeasonRepository;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.feed.IEventSourceParser;
import self.me.matchday.feed.RemoteEventRepository;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.feed.blogger.galataman.GalatamanPostParser;
import self.me.matchday.feed.blogger.zkfootball.ZKFPostParser;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventSource;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

/**
 * Class to pre-populate database with data from remote Galataman source.
 */
@Configuration
public class LoadEventData {

  private static final String LOG_TAG = "LoadEventData";

  private static final String LIVE_URL =
      "http://192.168.0.101/soccer/testing/zkf_known_good.json";
  private static final String LOCAL_URL =
      "http://192.168.0.101/soccer/testing/galataman_known_good.json";

  // Repos
  private EventSourceRepository eventSourceRepository;
  private CompetitionRepository competitionRepository;
  private TeamRepository teamRepository;
  private SeasonRepository seasonRepository;
  private FixtureRepository fixtureRepository;
  private MatchRepository matchRepository;
  private HighlightShowRepository highlightShowRepository;

  @Bean
  CommandLineRunner initEventSources(EventSourceRepository eventSourceRepository,
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

    return args -> {

      // Create GalatamanHDF (test) repo
      final Blogger gmanBlog = Blogger.fromUrl(new URL(LOCAL_URL));
      final IEventSourceParser galatamanPostParser = new GalatamanPostParser();
      final RemoteEventRepository gmanRepo = RemoteEventRepository
          .fromBlogger(gmanBlog, galatamanPostParser);

      // Save EventSources to DB
      gmanRepo.getEventSources().forEach(this::saveEventSource);

      // Create ZKFootball (LIVE!) blog
      final Blogger zkfBlog = Blogger.fromUrl(new URL(LIVE_URL));
      final IEventSourceParser zkfPostParser = new ZKFPostParser();
      final RemoteEventRepository zkfRepo = RemoteEventRepository
          .fromBlogger(zkfBlog, zkfPostParser);

      // Save latest EventSources
      zkfRepo.getEventSources().forEach(this::saveEventSource);
    };
  }

  @Transactional
  private void saveEventSource(@NotNull EventSource eventSource) {
//

    final Event event = eventSource.getEvent();
    Log.i(LOG_TAG, "Saving Competition: " + competitionRepository.save(event.getCompetition()));
    Log.i(LOG_TAG, "Saving Season: " + seasonRepository.saveAndFlush(event.getSeason()) );
    Log.i(LOG_TAG, "Saving Fixture: " + fixtureRepository.saveAndFlush(event.getFixture()) );

    if (event instanceof Match) {
      final Match match = (Match) event;
      Log.i(LOG_TAG, "Saving MATCH: " + matchRepository.save(match) );
      Log.i(LOG_TAG, "Saving Team: " + teamRepository.save(match.getHomeTeam()) );
      Log.i(LOG_TAG, "Saving Team: " + teamRepository.save(match.getAwayTeam()) );
    } else {
      final HighlightShow highlightShow = (HighlightShow) event;
      Log.i(LOG_TAG, "Saving HIGHLIGHT: " + highlightShowRepository.saveAndFlush(highlightShow) );
    }
    // Finally, save EventSource
    Log.i(LOG_TAG, "Saving EVENTSOURCE: " + eventSourceRepository.saveAndFlush(eventSource));
  }
}
