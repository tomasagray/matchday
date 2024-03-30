package self.me.matchday.plugin.datasource.forum;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.datasource.parsing.HypertextEntityParser;

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
