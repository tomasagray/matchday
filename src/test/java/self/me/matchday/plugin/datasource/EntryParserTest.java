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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import self.me.matchday.PrettyDataPrinter;
import self.me.matchday.model.Event;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.util.Log;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName(
    "Test that the EntryParser API can successfully parse Events & related from HTML given a PatternKit")
class EntryParserTest {

  private static final String LOG_TAG = "EntryParserTest";

  @ParameterizedTest(name = "#{index} Testing with Entry: {0}")
  @MethodSource("self.me.matchday.TestEventDataFromBlogger#getEventArgs")
  @DisplayName("Validate Event parsing")
  void testEvent(Event event) {

    Log.i(LOG_TAG, "Testing Event:");
    Log.i(LOG_TAG, PrettyDataPrinter.getPrintableEvent(event));

    assertThat(event).isNotNull();
    assertThat(event.getCompetition().getName()).isNotNull().isNotEmpty();
    assertThat(event.getHomeTeam().getName()).isNotNull().isNotEmpty();
    assertThat(event.getAwayTeam().getName()).isNotNull().isNotEmpty();
    assertThat(event.getDate()).isAfter(LocalDate.EPOCH.atStartOfDay());

    final Set<VideoFileSource> fileSources = event.getFileSources();
    assertThat(fileSources.size()).isGreaterThanOrEqualTo(2);
  }

  @ParameterizedTest(name = "#{index} Testing with VideoFileSource: {0}")
  @MethodSource("self.me.matchday.TestEventDataFromBlogger#getFileSourceArgs")
  @DisplayName("Validate File Source information parsed from Entry")
  void testFileSource(VideoFileSource fileSource) {

    Log.i(LOG_TAG, "Testing file source: " + PrettyDataPrinter.getPrintableFileSource(fileSource));

    assertThat(fileSource).isNotNull();
    assertThat(fileSource.getChannel()).isNotNull().isNotEmpty();
    assertThat(fileSource.getSource()).isNotNull().isNotEmpty();
    assertThat(fileSource.getResolution()).isNotNull();
    assertThat(fileSource.getMediaContainer()).isNotNull().isNotEmpty();
    assertThat(fileSource.getFileSize()).isNotNull().isNotZero();
    assertThat(fileSource.getLanguages()).isNotNull().isNotEmpty();
    assertThat(fileSource.getVideoBitrate()).isNotNull().isNotZero();
    assertThat(fileSource.getAudioChannels()).isNotZero();
    assertThat(fileSource.getFrameRate()).isNotZero();
    assertThat(fileSource.getAudioCodec()).isNotNull().isNotEmpty();

    final VideoFilePack videoFiles = fileSource.getVideoFilePacks().stream().findAny().orElse(null);
    assertThat(videoFiles).isNotNull();
    assertThat(videoFiles.size()).isGreaterThanOrEqualTo(2);
  }

  @ParameterizedTest(name = "#{index} Testing with VideoFile: {0}")
  @MethodSource("self.me.matchday.TestEventDataFromBlogger#getVideoFileArgs")
  @DisplayName("Validate VideoFile parsing")
  void testVideoFile(VideoFile file) {

    Log.i(LOG_TAG, "Testing video file: " + file);
    assertThat(file).isNotNull();
    assertThat(file.getExternalUrl()).isNotNull();
    assertThat(file.getTitle()).isNotNull();
  }
}
