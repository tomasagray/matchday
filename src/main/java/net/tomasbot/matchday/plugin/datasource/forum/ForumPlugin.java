package net.tomasbot.matchday.plugin.datasource.forum;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.plugin.datasource.DataSourcePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ForumPlugin implements DataSourcePlugin {

  private static final String PAGE_KEY = "page";

  private final ForumPluginProperties pluginProperties;
  private final ForumDataSourceValidator dataSourceValidator;
  private final EventListParser eventListParser;
  private final EventReader eventReader;

  public ForumPlugin(
      ForumPluginProperties pluginProperties,
      ForumDataSourceValidator dataSourceValidator,
      EventListParser eventListParser,
      EventReader eventReader) {
    this.pluginProperties = pluginProperties;
    this.dataSourceValidator = dataSourceValidator;
    this.eventListParser = eventListParser;
    this.eventReader = eventReader;
  }

  private static boolean isValidEvent(@NotNull Event event) {
    return event.getCompetition() != null && event.getDate() != null;
  }

  private static @NotNull Map<String, List<String>> getQueryParams(@NotNull URL url) {
    Map<String, List<String>> params = new HashMap<>();
    String[] urlParts = url.toString().split("\\?");
    if (urlParts.length < 2) {
      return params;
    }

    String query = urlParts[1];
    for (String param : query.split("&")) {
      String[] pair = param.split("=");
      String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
      String value = "";
      if (pair.length > 1) {
        value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
      }

      // skip ?& and &&
      if ("".equals(key) && pair.length == 1) {
        continue;
      }

      List<String> values = params.computeIfAbsent(key, k -> new ArrayList<>());
      values.add(value);
    }

    return params;
  }

  private static String getQuery(@NotNull Map<String, List<String>> params) {
    return params.entrySet().stream()
        .flatMap(
            param -> param.getValue().stream().map(v -> String.format("%s=%s", param.getKey(), v)))
        .collect(Collectors.joining("&"));
  }

  private static int getCurrentPage(@NotNull List<String> pages) {
    final int DEFAULT_PAGE = 0;
    if (pages.isEmpty()) return DEFAULT_PAGE;
    final String page = pages.get(0);
    return page.matches("\\d") ? Integer.parseInt(page) : DEFAULT_PAGE;
  }
  
  @Override
  @SuppressWarnings("unchecked cast")
  public <T> Snapshot<T> getSnapshot(
      @NotNull SnapshotRequest request, @NotNull DataSource<T> dataSource) throws IOException {
    validateDataSource(dataSource);
    DataSource<? extends Event> eventDataSource = (DataSource<? extends Event>) dataSource;

    URL url = eventDataSource.getBaseUri().toURL();
    Stream<T> events = Stream.empty();

    int scrapeSteps = pluginProperties.getScrapeSteps();
    // subtract 1 to account for the initial 'do' loop
    final AtomicInteger steps = new AtomicInteger(scrapeSteps - 1);

    int newEventCount;
    do {
      // read remote data
      List<T> newEvents = (List<T>) getEventStream(url, eventDataSource).toList();

      newEventCount = newEvents.size();
      events = Stream.concat(events, newEvents.stream());
      url = parseNextLink(url);

    } while (newEventCount > 0 && steps.getAndDecrement() > 0 && url != null);

    return Snapshot.of(events);
  }

  @SuppressWarnings("unchecked cast")
  @NotNull
  private <T> Stream<T> getEventStream(
      @NotNull URL url, DataSource<? extends Event> eventDataSource) throws IOException {
    String data = RemoteDataReader.readDataFrom(url);
    return (Stream<T>) readEventStream(data, eventDataSource);
  }

  private @Nullable URL parseNextLink(@NotNull URL url) throws MalformedURLException {
    Map<String, List<String>> params = getQueryParams(url);
    String currentPageQuery = getQuery(params);
    List<String> pages = params.remove(PAGE_KEY);
    if (pages != null && !pages.isEmpty()) {
      int currentPage = getCurrentPage(pages);
      params.put(PAGE_KEY, List.of(++currentPage + ""));
      String nextPageQuery = getQuery(params);
      String nextPageUrl = url.toString().replace(currentPageQuery, nextPageQuery);
      return new URL(nextPageUrl);
    }

    // not a linkable URL
    return null;
  }

  private @NotNull Stream<Event> readEventStream(
      @NotNull String data, @NotNull DataSource<? extends Event> dataSource) {
    return eventListParser.getEventsList(data, dataSource).entrySet().stream()
        .filter(entry -> isValidEvent(entry.getValue()))
        .map(entry -> eventReader.readListEvent(entry, dataSource))
        .map(CompletableFuture::join)
        .filter(Objects::nonNull);
  }

  @Override
  public void validateDataSource(@NotNull DataSource<?> dataSource) {
    dataSourceValidator.validateDataSourcePluginId(this.getPluginId(), dataSource.getPluginId());
    dataSourceValidator.validateDataSourceType(dataSource);
    dataSourceValidator.validateDataSourcePatternKits((PlaintextDataSource<?>) dataSource);
  }

  @Override
  @SuppressWarnings("unchecked cast")
  public <T> Snapshot<T> getUrlSnapshot(@NotNull URL url, @NotNull DataSource<T> dataSource)
      throws IOException {
    Event event = eventReader.readEvent(url, (DataSource<? extends Event>) dataSource);
    return Snapshot.of(Stream.of((T) event));
  }

  @Override
  public UUID getPluginId() {
    return UUID.fromString(pluginProperties.getId());
  }

  @Override
  public String getTitle() {
    return pluginProperties.getTitle();
  }

  @Override
  public String getDescription() {
    return pluginProperties.getDescription();
  }
}
