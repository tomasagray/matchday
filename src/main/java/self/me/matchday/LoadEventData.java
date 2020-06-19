/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday;

import java.io.IOException;
import java.net.URL;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import self.me.matchday._DEVFIXTURES.URLs;
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

  @Bean
  CommandLineRunner initEventSources(EventService eventService) {

    return args -> {
      saveEventStream(eventService, createGalatamanRepo(URLs.GMAN_HTML_KNOWN_GOOD));
      saveEventStream(eventService, createGalatamanRepo(URLs.GMAN_HTML_KNOWN_GOOD_NEXT));
//      saveEventStream(eventService, createZKFRepo(URLs.ZKF_JSON_LIVE));
    };
  }

  private void saveEventStream(EventService eventService,
      @NotNull RemoteEventRepository repository) {

    repository
        .getEventStream()
        .forEach(event -> Log.i(LOG_TAG, "Saving Event: " + eventService.saveEvent(event)));
  }

  private @NotNull RemoteEventRepository createZKFRepo(@NotNull final URL url) throws IOException {
    // Create ZKF repo
    final Blogger zkfBlogger = BloggerFactory.fromJson(url);
    return BloggerRepoFactory.createRepository(zkfBlogger, new ZKFParserFactory());
  }

  private @NotNull RemoteEventRepository createGalatamanRepo(@NotNull final URL url) throws IOException {
    // Create GMan repo
    final Blogger gmanBlogger = BloggerFactory.fromHtml(url);
    return BloggerRepoFactory.createRepository(gmanBlogger, new GalatamanParserFactory());
  }
}
