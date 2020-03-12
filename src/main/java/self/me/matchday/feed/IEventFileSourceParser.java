package self.me.matchday.feed;

import java.util.List;
import self.me.matchday.model.EventFileSource;

public interface IEventFileSourceParser {

  List<EventFileSource> getEventFileSources();
}
