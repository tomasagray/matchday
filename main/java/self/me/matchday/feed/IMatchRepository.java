package self.me.matchday.feed;

import java.util.stream.Stream;

public interface IMatchRepository {

  /**
   * Return a Stream of all Matches from this repository.
   *
   * @return A Stream<> of IMatchSource (Match sources)
   */
  Stream<IMatchSource> getMatches();
}
