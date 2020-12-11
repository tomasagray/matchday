/*
 * Copyright (c) 2020.
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("Testing for file-exists checker")
class FileCheckTaskTest {

  private static final String LOG_TAG = "FileCheckTaskTest";

  // Test resource
  private static Path playlistPath;

  @BeforeAll
  static void setup() {

    // Create test resource
    final URL testPlaylistUrl =
        FileCheckTaskTest.class.getClassLoader().getResource("test-playlist.m3u8");
    assertThat(testPlaylistUrl).isNotNull();
    playlistPath = new File(testPlaylistUrl.getFile()).toPath();
  }

  @Test
  @DisplayName("Validate correctly finds specified file within time limit")
  void testPlaylistCheck() throws InterruptedException {

    // Create check task
    final FileCheckTask testTask = new FileCheckTask(playlistPath.toFile(), 250);
    // Run task
    testTask.start();
    // Wait until finished
    testTask.join();

    // Test result
    final boolean testResult = testTask.isFileFound();
    Log.i(LOG_TAG, "File was found? " + testResult);
    assertThat(testResult).isTrue();
  }
}
