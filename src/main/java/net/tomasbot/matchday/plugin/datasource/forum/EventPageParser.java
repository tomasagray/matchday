package net.tomasbot.matchday.plugin.datasource.forum;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.plugin.datasource.parsing.HypertextEntityParser;

@Component
public class EventPageParser {

  private final HypertextEntityParser entityParser;

  public EventPageParser(HypertextEntityParser entityParser) {
    this.entityParser = entityParser;
  }

  public Event getEventFrom(@NotNull DataSource<? extends Event> dataSource, @NotNull String data) {
    return entityParser.getEntityStream(dataSource, data).findFirst().orElse(null);
  }
}
