package net.tomasbot.matchday.plugin.datasource.forum;

import java.net.URI;
import java.util.Collection;
import lombok.Builder;
import lombok.Data;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.SecureCookie;

@Data
@Builder
public class EventMetaDataRequest {
  private URI uri;
  private Event event;
  private Collection<SecureCookie> cookies;
  private DataSource<? extends Event> dataSource;
}
