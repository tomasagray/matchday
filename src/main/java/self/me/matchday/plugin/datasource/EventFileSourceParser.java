package self.me.matchday.plugin.datasource;

import java.util.List;
import self.me.matchday.model.EventFileSource;

public interface EventFileSourceParser {

  List<EventFileSource> getEventFileSources();
}
