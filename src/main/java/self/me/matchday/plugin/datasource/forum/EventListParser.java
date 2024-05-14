package self.me.matchday.plugin.datasource.forum;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import self.me.matchday.model.*;
import self.me.matchday.plugin.datasource.parsing.TextParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EventListParser {

    private final String linkSelector;
    private final TextParser textParser;

    public EventListParser(ForumPluginProperties pluginProperties, TextParser textParser) {
        this.linkSelector = pluginProperties.getLinkSelector();
        this.textParser = textParser;
    }

    public Map<URI, ? extends Event> getEventsList(@NotNull String data, DataSource<? extends Event> dataSource) {
        Document document = Jsoup.parse(data);
        Elements links = document.select(this.linkSelector);
        return links.stream()
                .collect(Collectors.toMap(this::getLinkHref, link -> parseMatchLink(link, dataSource), (e1, e2) -> e1));
    }

    private @Nullable URI getLinkHref(@NotNull Element link) {
        try {
            return new URI(link.attr("href"));
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private @NotNull Event parseMatchLink(@NotNull Element link, @NotNull DataSource<? extends Event> dataSource) {
        if (!(dataSource instanceof PlaintextDataSource<? extends Event>)) {
            String msg = String.format(
                    "Cannot parse Event data; %s is not a Plaintext datasource: ", dataSource.getDataSourceId());
            throw new IllegalArgumentException(msg);
        }
        List<PatternKit<? extends Match>> patternKits =
                ((PlaintextDataSource<? extends Event>) dataSource).getPatternKitsFor(Match.class);
        final String text = link.text();
        Optional<? extends Event> optionalEvent = textParser.createEntityStreams(patternKits, text).findFirst();
        return optionalEvent.isPresent() ? optionalEvent.get() : new Match();
    }
}
