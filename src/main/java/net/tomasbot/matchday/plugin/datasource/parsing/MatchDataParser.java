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

package net.tomasbot.matchday.plugin.datasource.parsing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Match;
import net.tomasbot.matchday.model.PatternKit;
import net.tomasbot.matchday.model.PlaintextDataSource;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoFileSource;
import net.tomasbot.matchday.plugin.datasource.parsing.fabric.Bolt;

@Component
public class MatchDataParser implements DataSourceParser<Match, String> {

  private final TextParser textParser;
  @Getter private final Class<Match> type = Match.class;

  public MatchDataParser(TextParser textParser) {
    this.textParser = textParser;
  }

  @SuppressWarnings("unchecked cast")
  @Override
  public Stream<? extends Match> getEntityStream(
      @NotNull DataSource<? extends Match> dataSource, @NotNull String data) {
    final Document document = Jsoup.parse(data);
    final String text = document.text();

    final PlaintextDataSource<Match> plaintextDataSource = (PlaintextDataSource<Match>) dataSource;
    final List<PatternKit<? extends Match>> eventPatternKits =
        plaintextDataSource.getPatternKitsFor(Match.class);
    final Stream<? extends Match> eventStream = getStreamForType(eventPatternKits, text);
    final Stream<? extends VideoFileSource> fileSourceStream =
        getStreamForType(plaintextDataSource.getPatternKitsFor(VideoFileSource.class), text);
    final Stream<VideoFile> videoFileStream =
        getStreamForType(plaintextDataSource.getPatternKitsFor(VideoFile.class), text);
    final Stream<URL> links =
        createUrlStreams(plaintextDataSource.getPatternKitsFor(URL.class), document);

    return Bolt.of(links)
        .zipInto(videoFileStream, VideoFile::setExternalUrl)
        .foldInto(fileSourceStream, new VideoFilePackFolder<>(), VideoFileSource::addVideoFilePack)
        .foldInto(eventStream, new ListFolder<>(), Match::addAllFileSources)
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
