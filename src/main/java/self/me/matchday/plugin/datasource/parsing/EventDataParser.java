/*
 * Copyright (c) 2022.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.datasource.parsing;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.PatternKit;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.plugin.datasource.parsing.fabric.Bolt;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
public class EventDataParser implements DataSourceParser<Event, String> {

  private final TextParser textParser;

  public EventDataParser(TextParser textParser) {
    this.textParser = textParser;
  }

  @Override
  public Stream<? extends Event> getEntityStream(
      @NotNull DataSource<Event> dataSource, @NotNull String data) {

    final Document document = Jsoup.parse(data);
    final String text = document.text();

    final PlaintextDataSource<Event> plaintextDataSource = (PlaintextDataSource<Event>) dataSource;
    final Stream<? extends Event> eventStream =
        getStreamForType(plaintextDataSource.getPatternKitsFor(Event.class), text);
    final Stream<? extends VideoFileSource> fileSourceStream =
        getStreamForType(plaintextDataSource.getPatternKitsFor(VideoFileSource.class), text);
    final Stream<VideoFile> videoFileStream =
        getStreamForType(plaintextDataSource.getPatternKitsFor(VideoFile.class), text);
    final Stream<URL> links =
        createUrlStreams(plaintextDataSource.getPatternKitsFor(URL.class), document);

    return Bolt.of(links)
        .zipInto(videoFileStream, VideoFile::setExternalUrl)
        .foldInto(fileSourceStream, new VideoFilePackFolder<>(), VideoFileSource::addVideoFilePack)
        .foldInto(eventStream, new ListFolder<>(), Event::addAllFileSources)
        .stream();
  }

  @SuppressWarnings("unchecked cast")
  private <T> Stream<T> getStreamForType(
      @NotNull List<PatternKit<? extends T>> patternKits, String data) {
    return (Stream<T>) textParser.createEntityStreams(patternKits, data);
  }

  private Stream<URL> createUrlStreams(
      @NotNull Collection<PatternKit<? extends URL>> patternKits, @NotNull Document document) {

    Stream<URL> base = Stream.empty();
    for (PatternKit<? extends URL> patternKit : patternKits) {
      final Stream<URL> urlStream = createUrlStream(document, patternKit);
      if (urlStream != null) {
        base = Stream.concat(base, urlStream);
      } else {
        break;
      }
    }
    return base;
  }

  private Stream<URL> createUrlStream(
      @NotNull Document document, @NotNull PatternKit<? extends URL> patternKit) {

    final Pattern urlPattern = patternKit.getPattern();
    final String query = String.format("a[href~=%s]", urlPattern);
    return document.select(query).stream()
        .map(link -> link.attr("href"))
        .map(
            link -> {
              try {
                return new URL(link);
              } catch (MalformedURLException e) {
                return null;
              }
            });
  }
}
