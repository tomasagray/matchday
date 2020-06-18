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
import self.me.matchday.feed.blogger.BloggerFactory;
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

  private static final String ZKF_JSON_LIVE =
      "https://zkfootballmatch.blogspot.com/feeds/posts/default?alt=json";
  // safe copy
  private static final String ZKF_JSON_CACHED =
      "http://192.168.0.101/soccer/testing/zkf_known_good.json";

  private static final String GALATAMAN_JSON_CACHED =
      "http://192.168.0.101/soccer/testing/galataman_known_good.json";
  private static final String GALATAMAN_HTML_LIVE =
      "https://galatamanhdfb.blogspot.com/search/label/EPL";

  @Bean
  CommandLineRunner initEventSources(EventService eventService) {

    return args -> {

      // Create GMan repo
      final Blogger gmanBlogger = BloggerFactory.fromHtml(new URL(GALATAMAN_HTML_LIVE));
      final RemoteEventRepository gmanRepo =
          BloggerRepoFactory.createRepository(gmanBlogger, new GalatamanParserFactory());

      // Create ZKF repo
      final Blogger zkfBlogger = BloggerFactory.fromJson(new URL(ZKF_JSON_LIVE));
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
