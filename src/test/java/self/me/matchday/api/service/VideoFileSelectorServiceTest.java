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
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFile.EventPartIdentifier;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;
import self.me.matchday.util.Log;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for VideoFile 'best version' selector microservice")
class VideoFileSelectorServiceTest {

  private static final String LOG_TAG = "VideoFileSelectorServiceTest";

  private static VideoFileSource testVideoFileSource;
  private static VideoFileSelectorService fileSelectorService;

  @BeforeAll
  static void setUp(
      @Autowired @NotNull final TestDataCreator testDataCreator,
      @Autowired @NotNull final VideoFileSelectorService fileSelectorService) {

    VideoFileSelectorServiceTest.fileSelectorService = fileSelectorService;

    // Create test data
    testVideoFileSource = testDataCreator.createTestVideoFileSource();
    // Set internal urls for testing
    setInternalUrls(testVideoFileSource.getVideoFilePacks());
  }

  private static void setInternalUrls(@NotNull final List<VideoFilePack> videoFiles) {
    videoFiles.forEach(
        pack ->
            pack.forEach(
                (title, videoFile) -> videoFile.setInternalUrl(videoFile.getExternalUrl())));
  }

  @Test
  @DisplayName("Validate service correctly chooses VideoFiles")
  void getPlaylistFiles() {

    // Test parameters
    final int expectedVideoFileCount = 4;

    Log.i(LOG_TAG, "Testing VideoFileSource: " + testVideoFileSource);
    final VideoFilePack testPlaylistFiles =
        fileSelectorService.getPlaylistFiles(testVideoFileSource);

    final int actualVideoFileCount = testPlaylistFiles.size();
    assertThat(actualVideoFileCount).isEqualTo(expectedVideoFileCount);
    testPlaylistFiles.forEach(
        (title, videoFile) -> {
          Log.i(LOG_TAG, "Got VideoFile: " + videoFile);
          final URL internalUrl = videoFile.getInternalUrl();
          Log.i(LOG_TAG, "Internal URL: " + internalUrl);
          assertThat(videoFile).isNotNull();
          assertThat(internalUrl).isNotNull();
        });
  }

  @Test
  @DisplayName("Validate order of VideoFiles returned by service")
  void testVideoFileOrder() {

    final VideoFilePack testFileList = fileSelectorService.getPlaylistFiles(testVideoFileSource);
    Log.i(LOG_TAG, "Testing event file order for: " + testFileList);

    final VideoFile preMatch = testFileList.get(PartIdentifier.PRE_MATCH);
    final VideoFile firstHalf = testFileList.get(PartIdentifier.FIRST_HALF);
    final VideoFile secondHalf = testFileList.get(PartIdentifier.SECOND_HALF);
    final VideoFile postMatch = testFileList.get(PartIdentifier.POST_MATCH);

    assertThat(preMatch.getTitle()).isEqualTo(PartIdentifier.PRE_MATCH);
    assertThat(firstHalf.getTitle()).isEqualTo(PartIdentifier.FIRST_HALF);
    assertThat(secondHalf.getTitle()).isEqualTo(PartIdentifier.SECOND_HALF);
    assertThat(postMatch.getTitle()).isEqualTo(PartIdentifier.POST_MATCH);
  }
}
