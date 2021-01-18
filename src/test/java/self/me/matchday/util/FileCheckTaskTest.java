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

package self.me.matchday.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("Testing for file-exists checker")
class FileCheckTaskTest {

  public static final String TEST_IMG = "check-task-test.jpg";
  public static final String TEST_URL = "https://wallpapercave.com/wp/wp4127639.jpg";
  public static final int CHECK_INTERVAL_MS = 250;
  public static final int MAX_DL_SECONDS = 5;
  public static final int MIN_DL_MILLIS = 500;
  private static final String LOG_TAG = "FileCheckTaskTest";
  // Test resource
  private static Path playlistPath;
  private static File testDownloadImg;

  @BeforeAll
  static void setup() {

    // Create test resource
    final URL testPlaylistUrl =
        FileCheckTaskTest.class.getClassLoader().getResource("test-playlist.m3u8");
    assertThat(testPlaylistUrl).isNotNull();
    playlistPath = new File(testPlaylistUrl.getFile()).toPath();
  }

  @AfterAll
  static void tearDown() throws IOException {

    // Delete downloaded  resource
    if (testDownloadImg != null && testDownloadImg.exists()) {
      Files.delete(testDownloadImg.toPath());
    }
  }

  @Test
  @DisplayName("Validate correctly finds already existing file within time limit")
  void testPlaylistCheck() throws InterruptedException {

    // Create check task
    final FileCheckTask testTask = new FileCheckTask(playlistPath.toFile(), CHECK_INTERVAL_MS);
    // Run task
    testTask.start();
    // Wait until finished
    testTask.join();

    // Test result
    final boolean testResult = testTask.isFileFound();
    final File testFile = testTask.getFile();
    final Duration actualExecutionTime = testTask.getExecutionTime();
    Log.i(LOG_TAG, String.format("File: %s was found? %s", testFile.getAbsolutePath(), testResult));
    Log.i(LOG_TAG, "Execution time: " + actualExecutionTime);
    assertThat(testResult).isTrue();
    assertThat(actualExecutionTime).isLessThanOrEqualTo(Duration.ofSeconds(3));
  }

  @Test
  @DisplayName("Validate correctly determines time to download remote image resource")
  void testRemoteFileTransfer() throws IOException, InterruptedException {

    final URL testUrl = new URL(TEST_URL);
    // Local storage location - parent of test resource
    final Path storagePath = playlistPath.getParent();
    testDownloadImg = storagePath.resolve(TEST_IMG).toFile();

    // Start check task
    final FileCheckTask fileCheckTask = new FileCheckTask(testDownloadImg, CHECK_INTERVAL_MS);
    fileCheckTask.start();

    // Begin remote resource download
    try (final InputStream is = testUrl.openStream()) {
      Files.copy(is, testDownloadImg.toPath());
    }
    // Finish check
    fileCheckTask.join();

    // Perform test
    final Duration expectedMaxExecutionTime = Duration.ofSeconds(MAX_DL_SECONDS);
    final Duration expectedMinExecutionTime = Duration.ofMillis(MIN_DL_MILLIS);
    final Duration actualExecutionTime = fileCheckTask.getExecutionTime();
    Log.i(LOG_TAG, "Download execution time: " + actualExecutionTime);
    assertThat(actualExecutionTime)
        .isLessThanOrEqualTo(expectedMaxExecutionTime)
        .isGreaterThanOrEqualTo(expectedMinExecutionTime);
  }
}
