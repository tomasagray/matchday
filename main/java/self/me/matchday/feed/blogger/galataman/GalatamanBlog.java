/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger.galataman;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;
import self.me.matchday.feed.IEventRepository;
import self.me.matchday.feed.EventSource;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.feed.blogger.IBloggerPostProcessor;

/**
 * Represents the Galataman HDF football match blog, which can be found at:
 *  https://galatamanhdf.blogspot.com
 */
public class GalatamanBlog extends Blogger implements IEventRepository {

  public GalatamanBlog(URL url, IBloggerPostProcessor postProcessor) throws IOException {
    super(url, postProcessor);
  }

  @Override
  public Stream<EventSource> getEventSources() {
    // Return all Blogger entries (BloggerPost) as IMatchSources
    return getEntries().stream().map(entry -> entry);
  }
}
