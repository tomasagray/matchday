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

package self.me.matchday.plugin.datasource;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import self.me.matchday.model.*;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFile.EventPartIdentifier;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.model.video.VideoFileSource.Resolution;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit.EventMetadataPatternKit;

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static self.me.matchday.model.video.VideoSourceMetadataPatternKit.FileSourceMetadataPatternKit;

/**
 * Parses a given BloggerEntry into an Event (with related VideoFileSources, etc.) using the
 * VideoSourceMetadataPatternKit supplied by the accompanying PatternKitAdapter.
 */
public class EntryParser {

  private final Document data;
  private final VideoSourceMetadataPatternKit patternKit;

  private EntryParser(@NotNull String html, @NotNull VideoSourceMetadataPatternKit patternKit) {

    this.data = Jsoup.parse(html);
    this.patternKit = patternKit;
  }

  @Contract(value = "_ -> new", pure = true)
  public static @NotNull PatternKitAdapter parse(String html) {
    return new PatternKitAdapter(html);
  }

  private @NotNull Event getEvent() {

    final String text = data.text();
    final Event event = parseEventData(text);
    final List<VideoFileSource> fileSources = parseVideoFileSources(text);
    event.addFileSources(fileSources);
    return event;
  }

  private @NotNull Event parseEventData(@NotNull final String text) {

    final EventMetadataPatternKit eventKit = patternKit.getEventMetadataPatternKit();
    final Pattern metadataRegex = eventKit.getEventMetadataRegex();
    final Matcher metadata = metadataRegex.matcher(text);

    if (metadata.find()) {
      final Team homeTeam = getNew(Team.class, metadata, eventKit.getHomeTeamName());
      final Team awayTeam = getNew(Team.class, metadata, eventKit.getAwayTeamName());
      final Competition competition =
          getNew(Competition.class, metadata, eventKit.getCompetitionName());
      final LocalDate date = parseDate(eventKit, metadata);
      Fixture fixture = parseFixture(eventKit, metadata);
      Season season = parseSeason(eventKit, metadata);
      return Event.builder()
          .homeTeam(homeTeam)
          .awayTeam(awayTeam)
          .competition(competition)
          .season(season)
          .fixture(fixture)
          .date(date.atStartOfDay())
          .build();
    }
    final String msg =
        String.format(
            "Could not parse Event metadata with pattern:%n%s%n from supplied text:%n%s",
            metadataRegex, text.replace("_", "").substring(0, 128));
    throw new IllegalArgumentException(msg);
  }

  private <T> @Nullable T getNew(@NotNull Class<T> clazz, @NotNull Matcher matcher, int group) {

    try {
      if (group == 0 || matcher.groupCount() < group) {
        return null;
      }
      final Constructor<T> constructor = clazz.getConstructor(String.class);
      final String name = matcher.group(group);
      if (name == null || "".equals(name)) {
        return null;
      }
      return constructor.newInstance(name);
    } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
      throw new RuntimeException(
          "Invalid class or data submitted for auto pattern parsing: " + clazz, e);
    }
  }

  private List<VideoFileSource> parseVideoFileSources(@NotNull final String text) {

    return patternKit.getFileSourceMetadataPatternKits().stream()
        .map(patternKit -> parseFileSourceFrom(text, patternKit))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @NotNull
  private List<VideoFileSource> parseFileSourceFrom(
      @NotNull String text, @NotNull FileSourceMetadataPatternKit patternKit) {

    final List<VideoFileSource> fileSources = new ArrayList<>();
    final Matcher matcher = patternKit.getFileSourceMetadataRegex().matcher(text);

    while (matcher.find()) {
      final VideoFileSource fileSource = createVideoFileSource(matcher, patternKit);
      fileSources.add(fileSource);
    }

    final List<VideoFilePack> videoFiles = parseVideoFiles(patternKit);
    mapVideoFilesToFileSources(videoFiles, fileSources);
    return fileSources;
  }

  private VideoFileSource createVideoFileSource(
      @NotNull Matcher matcher, @NotNull FileSourceMetadataPatternKit patternKit) {

    final String channel = getNew(String.class, matcher, patternKit.getChannel());
    final String res = getNew(String.class, matcher, patternKit.getResolution());
    final Resolution resolution = res != null ? Resolution.fromString(res) : Resolution.R_SD;
    final String source = getNew(String.class, matcher, patternKit.getSource());
    final String duration = getNew(String.class, matcher, patternKit.getDuration());
    final String languages = getNew(String.class, matcher, patternKit.getLanguages());
    final String container = getNew(String.class, matcher, patternKit.getContainer());
    final String vCodec = getNew(String.class, matcher, patternKit.getVideoCodec());
    final String aCodec = getNew(String.class, matcher, patternKit.getAudioCodec());
    final String aChannelData = getNew(String.class, matcher, patternKit.getAudioChannels());
    final int audioChannels = parseAudioChannels(aChannelData);
    final String vBitrateData = getNew(String.class, matcher, patternKit.getVideoBitrate());
    final Long vBitrate = vBitrateData != null ? Long.parseLong(vBitrateData) : 0;
    final String filesizeData = getNew(String.class, matcher, patternKit.getFilesize());
    final Long filesize = filesizeData != null ? Long.parseLong(filesizeData) : 0;
    final Integer framerateData = getNew(Integer.class, matcher, patternKit.getFramerate());
    final int framerate = framerateData != null ? framerateData : 0;

    return VideoFileSource.builder()
        .channel(channel)
        .resolution(resolution)
        .source(source)
        .approximateDuration(duration)
        .languages(languages)
        .mediaContainer(container)
        .videoCodec(vCodec)
        .audioCodec(aCodec)
        .audioChannels(audioChannels)
        .videoBitrate(vBitrate)
        .fileSize(filesize)
        .frameRate(framerate)
        .videoFilePacks(new ArrayList<>())
        .build();
  }

  private @NotNull List<VideoFilePack> parseVideoFiles(
      @NotNull FileSourceMetadataPatternKit patternKit) {

    final Pattern videoUrlPattern = patternKit.getVideoFileUrlRegex();
    final String query = String.format("a[href~=%s]", videoUrlPattern.toString());
    final Elements links = data.select("a");
    List<VideoFilePack> videoFiles = new ArrayList<>();
    links
        .select(query)
        .forEach(
            link -> {
              try {
                final EventPartIdentifier title =
                    findPartTitle(link, patternKit.getEventPartIdentifierRegex());
                final String href = link.attr("href");
                final URL url = new URL(href);
                final VideoFile videoFile = new VideoFile(title, url);
                putVideoFileInPack(videoFiles, videoFile);
              } catch (MalformedURLException ignored) {
              }
            });
    return videoFiles;
  }

  void putVideoFileInPack(@NotNull List<VideoFilePack> videoFiles, @NotNull VideoFile videoFile) {

    boolean added = false;
    for (VideoFilePack pack : videoFiles) {
      if (!added) {
        added = pack.put(videoFile);
      }
    }
    if (!added) {
      final VideoFilePack next = new VideoFilePack();
      next.put(videoFile);
      videoFiles.add(next);
    }
  }

  void mapVideoFilesToFileSources(
      @NotNull List<VideoFilePack> videoFilePacks, @NotNull List<VideoFileSource> fileSources) {

    for (int i = 0; i < videoFilePacks.size(); i++) {
      if (fileSources.size() > i) {
        final VideoFileSource fileSource = fileSources.get(i);
        fileSource.getVideoFilePacks().add(videoFilePacks.get(i));
      }
    }
  }

  @NotNull
  private Season parseSeason(@NotNull EventMetadataPatternKit eventKit, Matcher metadata) {

    Season season = new Season();

    final String seasonData = getNew(String.class, metadata, eventKit.getSeason());
    if (seasonData != null) {
      final List<Integer> years =
          Arrays.stream(seasonData.split("/"))
              .map(Integer::parseInt)
              .map(year -> (year < 100) ? ((year < 50) ? (year += 2_000) : (year += 1_900)) : year)
              .collect(Collectors.toList());
      if (years.size() == 2) {
        season = new Season(years.get(0), years.get(1));
      } else if (years.size() == 1) {
        final int begin = years.get(0);
        season = new Season(begin, begin + 1);
      }
    }
    return season;
  }

  private @NotNull Fixture parseFixture(
      @NotNull EventMetadataPatternKit eventKit, Matcher metadata) {
    Fixture fixture = new Fixture();
    final String fixtureNum = getNew(String.class, metadata, eventKit.getFixture());
    if (fixtureNum != null) {
      fixture = new Fixture(fixtureNum);
    }
    return fixture;
  }

  private LocalDate parseDate(@NotNull EventMetadataPatternKit eventKit, Matcher metadata) {
    final String rawDate = getNew(String.class, metadata, eventKit.getDate());
    return rawDate != null
        ? LocalDate.parse(rawDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        : LocalDate.now();
  }

  private int parseAudioChannels(String data) {
    if (data == null) {
      return 0;
    }
    return Arrays.stream(data.split("\\.")).mapToInt(Integer::parseInt).sum();
  }

  private EventPartIdentifier findPartTitle(@NotNull Element link, @NotNull Pattern partIdRegex) {

    return link.previousElementSiblings().stream()
        .map(Element::text)
        .map(text -> getPartIdentifier(text, partIdRegex))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(EventPartIdentifier.DEFAULT);
  }

  private EventPartIdentifier getPartIdentifier(
      @NotNull final String text, @NotNull Pattern partRegex) {

    final Matcher matcher = partRegex.matcher(text);
    EventPartIdentifier result = null;

    if (matcher.find()) {
      final String partIdData = matcher.group(1).toLowerCase(Locale.ROOT);
      if (partIdData.contains("pre")) {
        result = EventPartIdentifier.PRE_MATCH;
      } else if (partIdData.contains("1st") || partIdData.contains("first")) {
        result = EventPartIdentifier.FIRST_HALF;
      } else if (partIdData.contains("2nd") || partIdData.contains("second")) {
        result = EventPartIdentifier.SECOND_HALF;
      } else if (partIdData.contains("post")) {
        result = EventPartIdentifier.POST_MATCH;
      } else if (partIdData.contains("trophy")) {
        result = EventPartIdentifier.TROPHY_CEREMONY;
      }
    }
    return result;
  }

  public static class PatternKitAdapter {

    private final String html;

    PatternKitAdapter(String html) {
      this.html = html;
    }

    public Event with(@NotNull VideoSourceMetadataPatternKit patternKit) {
      return new EntryParser(this.html, patternKit).getEvent();
    }

    public Stream<Event> with(@NotNull Collection<VideoSourceMetadataPatternKit> patternKits) {
      return patternKits.stream().map(this::with);
    }
  }
}
