/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday;

import java.net.URL;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.db.FixtureRepository;
import self.me.matchday.db.HighlightShowRepository;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.db.SeasonRepository;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.feed.IEventSource;
import self.me.matchday.feed.blogger.galataman.GalatamanBlog;
import self.me.matchday.feed.blogger.galataman.GalatamanPostProcessor;
import self.me.matchday.model.Event;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

/** Class to pre-populate database with data from remote Galataman source. */
@Configuration
public class LoadEventData {

  private static final String LOG_TAG = "LoadEventData";

  private static final String GMAN_URL =
      "https://galatamanhdf.blogspot.com/feeds/posts/default/?alt=json";

  @Bean
  CommandLineRunner initEventData(
      MatchRepository matchRepository,
      HighlightShowRepository highlightShowRepository,
      TeamRepository teamRepository,
      CompetitionRepository competitionRepository,
      SeasonRepository seasonRepository,
      FixtureRepository fixtureRepository) {

    return args -> {
      // Get Events from remote server
      final GalatamanBlog galatamanBlog =
          new GalatamanBlog(new URL(GMAN_URL), new GalatamanPostProcessor());

      // Save each Event to local repo
      galatamanBlog
          .getEntries()
          .forEach(
              entry -> {
                final IEventSource eventSource = (IEventSource) entry;
                final Event event = eventSource.getEvent();

                if (event instanceof Match) {
                  // Cast to Match
                  final Match match = (Match) event;
                  // Save components first
                  Log.i(LOG_TAG, "Saving fixture: " + fixtureRepository.save(match.getFixture()));
                  Log.i(LOG_TAG, "Saving season: " + seasonRepository.save(match.getSeason()));
                  Log.i(LOG_TAG,"Saving competition: " + competitionRepository.save(match.getCompetition()));
                  Log.i(LOG_TAG, "Saving home team: " + teamRepository.save(match.getHomeTeam()));
                  Log.i(LOG_TAG, "Saving away team: " + teamRepository.save(match.getAwayTeam()));
                  Log.i(LOG_TAG, "Saving Match:\n" + matchRepository.save(match));

                } else if (event instanceof HighlightShow) {
                  final HighlightShow highlightShow = (HighlightShow) event;

                  Log.i(
                      LOG_TAG,
                      "Saving fixture: " + fixtureRepository.save(highlightShow.getFixture()));
                  Log.i(
                      LOG_TAG,
                      "Saving season: " + seasonRepository.save(highlightShow.getSeason()));
                  Log.i(
                      LOG_TAG,
                      "Saving competition: "
                          + competitionRepository.save(highlightShow.getCompetition()));
                  Log.i(
                      LOG_TAG,
                      "Saving Highlight Show: " + highlightShowRepository.save(highlightShow));
                } else {
                  Log.e(LOG_TAG, "COULD NOT DETERMINE EVENT TYPE!: " + event);
                }
              });
    };
  }
}
