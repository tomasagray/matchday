package self.me.matchday.plugin.blogger.parser;

import java.io.IOException;
import self.me.matchday.plugin.blogger.Blogger;

public interface BloggerBuilder {

  /**
   * Create a <b>Blogger</b> instance.
   *
   * @return An initialized Blogger instance
   */
  Blogger getBlogger() throws IOException;

}
