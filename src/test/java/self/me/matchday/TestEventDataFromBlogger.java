/*
 * Copyright (c) 2021.
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

package self.me.matchday;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.model.Event;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit;
import self.me.matchday.plugin.datasource.EntryParser;
import self.me.matchday.plugin.datasource.blogger.HtmlBloggerParser;
import self.me.matchday.plugin.datasource.blogger.model.Blogger;
import self.me.matchday.plugin.datasource.blogger.model.BloggerEntry;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static self.me.matchday.model.video.VideoSourceMetadataPatternKit.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TestEventDataFromBlogger {

  public static final Map<String, VideoSourceMetadataPatternKit> patternKits =
      createEventPatternKits();

  private static final String LOG_TAG = "TestEventDataFromBlogger";
  private static String html;

  static {
    try {
      //      html = getFreshHtml();
      html = getStaleHtml();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static @NotNull String getStaleHtml() throws IOException {

    final String dataLocation = "blogger_html_sample2.html";
    Log.i(LOG_TAG, "Reading test HTML data from: " + dataLocation);
    final String data =
        ResourceFileReader.readTextResource(TestEventDataFromBlogger.class, dataLocation);
    if (data == null) {
      throw new IOException("Could not read local test data");
    }
    return data;
  }

  public static @NotNull String getFreshHtml() throws IOException {

    final String dataLocation = "https://galatamanhdfb.blogspot.com";
    final URL remoteDataLocation = new URL(dataLocation);
    Log.i(LOG_TAG, "Reading remote data from: " + remoteDataLocation);
    return TextFileReader.readRemote(remoteDataLocation);
  }

  public static @NotNull @Unmodifiable Map<String, VideoSourceMetadataPatternKit>
      createEventPatternKits() {

    final FileSourceMetadataPatternKit fileSourcePatternKit = createFileSourcePatternKit();

    final Pattern eventPattern1 =
        Pattern.compile(
            "(?:FÚTBOL)?[:\\s]*([\\w\\s]*)(\\d{2,4}/\\d{2,4})?[\\s-]*[Matchdy]* (\\d)+[\\s-]*(\\d{2}"
                + "/\\d{2}/\\d{2,4}) ([\\w\\s-]+) vs.? ([\\w\\s-]+) _+");
    final EventMetadataPatternKit eventKit1 =
        EventMetadataPatternKit.builder()
            .eventMetadataRegex(eventPattern1)
            .competitionName(1)
            .homeTeamName(5)
            .awayTeamName(6)
            .season(2)
            .fixture(3)
            .date(4)
            .build();
    final VideoSourceMetadataPatternKit patternKit1 =
        from(eventKit1, List.of(fileSourcePatternKit));

    final Pattern eventPattern2 =
        Pattern.compile(
            "[FÚTBOL:]*([\\p{L}]+) (\\d{2,4}/\\d{2,4})[-Matchdy\\s]*(\\d)+[\\s-]*(\\d{2}/\\d{2}/\\d{2,4}) "
                + "([\\p{L}\\s-]+) vs.? ([\\p{L}\\s-]+) _+");
    final EventMetadataPatternKit eventKit2 =
        EventMetadataPatternKit.builder()
            .eventMetadataRegex(eventPattern2)
            .competitionName(1)
            .date(2)
            .homeTeamName(3)
            .awayTeamName(4)
            .build();
    final VideoSourceMetadataPatternKit patternKit2 =
        from(eventKit2, List.of(fileSourcePatternKit));

    return Map.of("WithFixture", patternKit1, "NoFixture", patternKit2);
  }

  // TODO: extract patterns
  public static FileSourceMetadataPatternKit createFileSourcePatternKit() {

    final Pattern fileSourcePattern =
        Pattern.compile(
            "Channel[\\s\\w]*:? ([\\w\\s+-]*) Source[\\w\\s]*:? ([\\w\\-]*) "
                + "Language[\\w\\s]*:? ([\\w\\s./]*) "
                + "Video[\\w\\s]*:? (\\d+) [KkMmbps]* ‖ (\\w\\.\\d+ \\w+) ‖ (\\d+)[fps]* "
                + "Audio[\\w\\s]*:? (\\d+)[\\sKkMmbps]+ ‖ (\\w+) ‖ ([\\d.]+) [chanelstro]* "
                + "Duration[\\w\\s]*:? (\\d+\\w*) Size[\\w\\s]*:? ~?(\\d+)[GgMmBb]* "
                + "Release[\\w\\s]*:? [HhDd]* (\\d+[pi])");
    final Pattern videoFileUrlRegex =
        Pattern.compile("^http[s]?://[\\w.]*filefox.cc/[\\w]+/[\\w-_]*.(mkv|ts)");
    final Pattern eventPartIdentifierRegex =
        Pattern.compile("(Pre-|Post-|1st|First|2nd|Second|Trophy)", Pattern.CASE_INSENSITIVE);

    return FileSourceMetadataPatternKit.builder()
        .fileSourceMetadataRegex(fileSourcePattern)
        .videoFileUrlRegex(videoFileUrlRegex)
        .eventPartIdentifierRegex(eventPartIdentifierRegex)
        .channel(1)
        .source(2)
        .languages(3)
        .videoBitrate(4)
        .container(5)
        .framerate(6)
        .audioBitrate(7)
        .audioCodec(8)
        .audioChannels(9)
        .duration(10)
        .filesize(11)
        .resolution(12)
        .build();
  }

  public static Stream<Arguments> getEventArgs() {
    return getEvents().map(Arguments::of);
  }

  public static Stream<Arguments> getFileSourceArgs() {
    return getVideoFileSources().map(Arguments::of);
  }

  public static Stream<Arguments> getVideoFileArgs() {
    return getVideoFiles().map(Arguments::of);
  }

  public static Stream<Event> getEvents() {

    final HtmlBloggerParser parser = new HtmlBloggerParser();
    final Blogger blogger = parser.getBlogger(html);
    return blogger.getFeed().getEntry().stream()
        .map(TestEventDataFromBlogger::parseBloggerEntry)
        .flatMap(Collection::stream);
  }

  public static @NotNull List<Event> parseBloggerEntry(@NotNull BloggerEntry entry) {

    final List<Event> events = new ArrayList<>();
    patternKits.forEach(
        (name, kit) -> {
          try {
            final String content = entry.getContent().getData();
            final Event event = EntryParser.parse(content).with(kit);
            assertThat(event).isNotNull();
            events.add(event);
          } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
          }
        });
    return events;
  }

  public static @NotNull Stream<VideoFileSource> getVideoFileSources() {
    return getEvents().map(Event::getFileSources).flatMap(Collection::stream);
  }

  public static @NotNull Stream<VideoFile> getVideoFiles() {
    return getVideoFileSources()
        .map(VideoFileSource::getVideoFilePacks)
        .flatMap(Collection::stream)
        .flatMap(VideoFilePack::stream);
  }
}
