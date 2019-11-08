package self.me.matchday.feed.galataman;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;
import self.me.matchday.feed.Blogger;
import self.me.matchday.feed.IBloggerPostProcessor;
import self.me.matchday.feed.IMatchRepository;
import self.me.matchday.feed.IMatchSource;
import self.me.matchday.fileserver.FSUser;
import self.me.matchday.fileserver.IFSManager;

public class GalatamanBlog extends Blogger implements IMatchRepository {
  // Fields
  private IFSManager ifsManager;
  private FSUser user;

  public GalatamanBlog(URL url, IBloggerPostProcessor postProcessor) throws IOException {
    super(url, postProcessor);
  }

  public void setFileServer(IFSManager ifsManager, FSUser user) {
    this.ifsManager = ifsManager;
    this.user = user;
  }

  @Override
  public Stream<IMatchSource> getMatches() {
    // Return all Blogger entries (BloggerPost) as IMatchSources
    return getEntries().stream().map(entry -> (IMatchSource) entry);
  }
}
