package net.tomasbot.matchday.plugin.datasource.forum;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.plugin.datasource.parsing.TextParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class EventListParser {

  private final TextParser textParser;

  public EventListParser(@NotNull TextParser textParser) {
    this.textParser = textParser;
  }

  private static void fail_dataType(@NotNull DataSource<? extends Event> dataSource) {
    String msg =
        String.format(
            "Cannot parse Event data: %s [%s] is not a Plaintext datasource: ",
            dataSource.getTitle(), dataSource.getPluginId());
    throw new IllegalArgumentException(msg);
  }

  private static PlaintextDataSource<? extends Event> validateDataSource(
      DataSource<? extends Event> dataSource) {
    if (dataSource instanceof PlaintextDataSource<? extends Event> plaintextDataSource) {
      // fields to be validated
      URI baseUri = plaintextDataSource.getBaseUri();
      String linkSelector = plaintextDataSource.getLinkSelector();

      if (baseUri == null || baseUri.toString().isBlank())
        throw new IllegalArgumentException("Datasource base URI cannot be null");

      if (linkSelector == null || linkSelector.isBlank())
        throw new IllegalArgumentException(
            "PlaintextDataSource link selector cannot be null/blank");

      return plaintextDataSource;
    } else
      throw new IllegalArgumentException(
          "Datasource is not a PlaintextDataSource: " + dataSource.getPluginId());
  }

  public Map<URI, ? extends Event> getEventsList(
      @NotNull String data, @NotNull DataSource<? extends Event> dataSource) {
    // convert to plaintext datasource
    PlaintextDataSource<? extends Event> plaintextDataSource = validateDataSource(dataSource);

    // fields were validated in validateDataSource()
    String linkSelector = plaintextDataSource.getLinkSelector();
    URI baseUri = plaintextDataSource.getBaseUri();
    assert linkSelector != null;
    assert baseUri != null;

    Document document = Jsoup.parse(data);
    Elements links = document.select(linkSelector);
    return links.stream()
        .collect(
            Collectors.toMap(
                link -> getLinkHref(link, baseUri),
                link -> parseMatchLink(link, dataSource),
                (e1, e2) -> e1));
  }

  private @Nullable URI getLinkHref(@NotNull Element link, @NotNull URI baseUri) {
    try {
      URI unencoded = new URI(link.attr("href"));

      // ensure special characters (e.g., á, ç, etc.) are properly escaped
      String escaped = unencoded.toASCIIString();
      URI encoded = new URI(escaped);

      return baseUri.resolve(encoded);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private @NotNull Event parseMatchLink(
      @NotNull Element link, @NotNull DataSource<? extends Event> dataSource) {
    if (!(dataSource instanceof PlaintextDataSource<? extends Event>)) fail_dataType(dataSource);

    final String text = link.text();
    List<PatternKit<? extends Match>> patternKits =
        ((PlaintextDataSource<? extends Event>) dataSource).getPatternKitsFor(Match.class);

    Optional<? extends Event> optionalEvent =
        textParser.createEntityStreams(patternKits, text).findFirst();

    return optionalEvent.isPresent() ? optionalEvent.get() : new Match();
  }
}
