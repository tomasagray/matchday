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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.CreateTestData;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.util.Log;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for EventFile 'best version' selector microservice")
class EventFileSelectorServiceTest {

  private static final String LOG_TAG = "EventFileSelectorServiceTest";

  private static EventFileSource testEventFileSource;
  private static EventFileSelectorService fileSelectorService;

  @BeforeAll
  static void setUp(@Autowired final EventFileSelectorService fileSelectorService) {

    EventFileSelectorServiceTest.fileSelectorService = fileSelectorService;

    // Create test data
    testEventFileSource = CreateTestData.createTestEventFileSource();
    // Set internal urls for testing
    setInternalUrls(testEventFileSource.getEventFiles());
    // Add unrefreshed EventFiles
    testEventFileSource.getEventFiles().addAll(CreateTestData.createTestEventFiles());
  }

  private static void setInternalUrls(@NotNull final List<EventFile> eventFiles) {
    eventFiles.forEach(eventFile -> eventFile.setInternalUrl(eventFile.getExternalUrl()));
  }

  @Test
  @DisplayName("Validate service correctly chooses EventFiles")
  void getPlaylistFiles() {

    // Test parameters
    final int expectedEventFileCount = 4;

    Log.i(LOG_TAG, "Testing EventFileSource: " + testEventFileSource);
    final List<EventFile> testPlaylistFiles =
        fileSelectorService.getPlaylistFiles(testEventFileSource);

    final int actualEventFileCount = testPlaylistFiles.size();
    assertThat(actualEventFileCount).isEqualTo(expectedEventFileCount);
    testPlaylistFiles.forEach(
        eventFile -> {
          Log.i(LOG_TAG, "Got EventFile: " + eventFile);
          final URL internalUrl = eventFile.getInternalUrl();
          Log.i(LOG_TAG, "Internal URL: " + internalUrl);
          assertThat(eventFile).isNotNull();
          assertThat(internalUrl).isNotNull();
        });
  }

  @Test
  @DisplayName("Validate order of EventFiles returned by service")
  void testEventFileOrder() {

    final List<EventFile> testFileList = fileSelectorService.getPlaylistFiles(testEventFileSource);
    Log.i(LOG_TAG, "Testing event file order for: " + testFileList);

    final EventFile preMatch = testFileList.get(0);
    final EventFile firstHalf = testFileList.get(1);
    final EventFile secondHalf = testFileList.get(2);
    final EventFile postMatch = testFileList.get(3);

    assertThat(preMatch.getTitle()).isEqualTo(EventPartIdentifier.PRE_MATCH);
    assertThat(firstHalf.getTitle()).isEqualTo(EventPartIdentifier.FIRST_HALF);
    assertThat(secondHalf.getTitle()).isEqualTo(EventPartIdentifier.SECOND_HALF);
    assertThat(postMatch.getTitle()).isEqualTo(EventPartIdentifier.POST_MATCH);
  }
}
