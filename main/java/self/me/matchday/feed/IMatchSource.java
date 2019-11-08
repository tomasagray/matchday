package self.me.matchday.feed;

import java.util.List;
import self.me.matchday.model.Match;

/**
 * Interface to allow reading of uniform data of source information for Matches.
 */
public interface IMatchSource {
  Match getMatch();
  List<IMatchFileSource> getMatchFileSources();
}
