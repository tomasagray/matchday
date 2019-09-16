package self.me.matchday.feed.galataman;

import java.io.IOException;
import java.net.URL;
import self.me.matchday.feed.Blogger;
import self.me.matchday.feed.IBloggerPostProcessor;

public class GalatamanBlog extends Blogger {

  public GalatamanBlog(URL url, IBloggerPostProcessor postProcessor)
      throws IOException {
    super(url, postProcessor);
  }
}
