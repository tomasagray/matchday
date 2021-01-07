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

package self.me.matchday.plugin.io.diskmanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.FileSize;
import self.me.matchday.util.Log;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("DiskManagerTest - Verify DiskManager plugin")
class DiskManagerTest {

  // Test constants
  private static final String LOG_TAG = "DiskManagerTest";

  // Test criteria
  private static final Long SPACE_ENOUGH_FOR = FileSize.ofGigabytes(1);
  private static final Long TOO_MUCH_SPACE = FileSize.ofGigabytes(Long.MAX_VALUE);

  private static DiskManager diskManager;

  @BeforeAll
  static void setUp(@Autowired final DiskManager diskManager) {
    DiskManagerTest.diskManager = diskManager;
  }

  @Test
  @DisplayName("Ensure accurately detects adequate free space")
  void isSpaceAvailable() throws IOException {

    boolean minSpaceAvailable;
    boolean tooMuchSpaceAvailable;

    // test
    minSpaceAvailable = diskManager.isSpaceAvailable(SPACE_ENOUGH_FOR);
    Log.i(
        LOG_TAG,
        String.format(
            "Ensuring at least %s bytes free... %s", SPACE_ENOUGH_FOR, minSpaceAvailable));
    assertThat(minSpaceAvailable).isTrue();

    tooMuchSpaceAvailable = diskManager.isSpaceAvailable(TOO_MUCH_SPACE);
    Log.i(
        LOG_TAG,
        String.format(
            "Ensuring there is NOT %s bytes free... %s", TOO_MUCH_SPACE, tooMuchSpaceAvailable));
    assertThat(tooMuchSpaceAvailable).isFalse();
  }

  @Test
  @DisplayName("Test free disk space computation")
  void getFreeDiskSpace() {

    final Long freeDiskSpace = diskManager.getFreeDiskSpace();
    Log.i(LOG_TAG, "Found free disk space: " + freeDiskSpace);

    // Ensure value is logical
    Assertions.assertNotEquals(0, freeDiskSpace);
    assertThat(freeDiskSpace).isGreaterThan(SPACE_ENOUGH_FOR);
  }

  @Test
  @DisplayName("Test used space computation")
  void getUsedSpace() {

    long usedSpace = 0;
    try {
      usedSpace = diskManager.getUsedSpace();
      Log.i(
          LOG_TAG,
          String.format(
              "Found %s bytes used in:\n\t%s\\", usedSpace, diskManager.getStorageLocation()));

    } catch (IOException e) {
      Log.e(LOG_TAG, "Error testing used disk space computation: " + e.getMessage());
    }

    // tests
    assertThat(usedSpace).isGreaterThan(0);
    assertThat(usedSpace).isLessThan(Long.MAX_VALUE);
  }
}
