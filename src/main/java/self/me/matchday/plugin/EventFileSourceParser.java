package self.me.matchday.plugin;

import java.util.List;
import self.me.matchday.model.EventFileSource;

public interface EventFileSourceParser {

  List<EventFileSource> getEventFileSources();
}
