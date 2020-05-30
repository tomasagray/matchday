/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday;

import java.net.URL;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import self.me.matchday.api.service.EventService;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.feed.blogger.BloggerRepoFactory;
import self.me.matchday.feed.blogger.galataman.GalatamanParserFactory;
import self.me.matchday.feed.blogger.zkfootball.ZKFParserFactory;
import self.me.matchday.model.RemoteEventRepository;
import self.me.matchday.util.Log;

/**
 * Class to pre-populate database with data from remote Galataman source.
 */
@Configuration
public class LoadEventData {

  private static final String LOG_TAG = "LoadEventData";

  private static final String LIVE_URL =
      "https://zkfootballmatch.blogspot.com/feeds/posts/default?alt=json"; // live URL
  //      "http://192.168.0.101/soccer/testing/zkf_known_good.json";  // safe copy
  private static final String LOCAL_URL =
      "http://192.168.0.101/soccer/testing/galataman_known_good.json";

  @Bean
  CommandLineRunner initEventSources(EventService eventService) {

    return args -> {

      // Create GMan repo
      final Blogger gmanBlogger = Blogger.fromJson(new URL(LOCAL_URL));
      final RemoteEventRepository gmanRepo =
          BloggerRepoFactory.createRepository(gmanBlogger, new GalatamanParserFactory());

      // Create ZKF repo
      final Blogger zkfBlogger = Blogger.fromJson(new URL(LIVE_URL));
      final RemoteEventRepository zkfRepo =
          BloggerRepoFactory.createRepository(zkfBlogger, new ZKFParserFactory());

      // Save Events
      gmanRepo.getEventStream()
          .forEach(event -> Log.i(LOG_TAG, "Saving Event: " + eventService.saveEvent(event)));
      zkfRepo.getEventStream()
          .forEach(event -> Log.i(LOG_TAG, "Saving Event: " + eventService.saveEvent(event)));
    };
  }
}
