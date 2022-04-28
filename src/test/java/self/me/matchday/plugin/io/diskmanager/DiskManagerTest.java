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

package self.me.matchday.plugin.io.diskmanager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.FileSize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("DiskManagerTest - Verify DiskManager plugin")
class DiskManagerTest {

  private static final Logger logger = LogManager.getLogger(DiskManagerTest.class);

  private static final Long SPACE_ENOUGH_FOR = FileSize.ofGigabytes(1);
  private static final Long TOO_MUCH_SPACE = FileSize.ofGigabytes(Long.MAX_VALUE);
  public static final int TEST_DATA_LINES = 10_000;

  private static DiskManager diskManager;

  @BeforeAll
  static void setUp(@Autowired @NotNull DiskManager diskManager) throws IOException {

    DiskManagerTest.diskManager = diskManager;

    // ensure test location exists
    Path storagePath = diskManager.getStorageLocation().toAbsolutePath();
    if (!Files.exists(storagePath)) {
      logger.warn("Test storage location: {} not found!\nCreating...", storagePath);
      Files.createDirectories(storagePath);
      // check again...
      assertThat(Files.exists(storagePath)).isTrue();
    }
  }

  @Test
  @DisplayName("Ensure accurately detects adequate free space")
  void isSpaceAvailable() throws IOException {

    boolean isSpaceAvailable;
    boolean tooMuchSpaceAvailable;

    logger.info("Ensuring at least {} bytes free...", SPACE_ENOUGH_FOR);
    isSpaceAvailable = diskManager.isSpaceAvailable(SPACE_ENOUGH_FOR);
    logger.info("Found  enough space? {}", isSpaceAvailable);
    assertThat(isSpaceAvailable).isTrue();

    tooMuchSpaceAvailable = diskManager.isSpaceAvailable(TOO_MUCH_SPACE);
    logger.info("Ensuring there is NOT too much (more than {}) free space", TOO_MUCH_SPACE);
    assertThat(tooMuchSpaceAvailable).isFalse();
  }

  @Test
  @DisplayName("Test free disk space computation")
  void getFreeDiskSpace() {

    final Long freeDiskSpace = diskManager.getFreeDiskSpace();
    logger.info("Found free disk space: {}", freeDiskSpace);

    // Ensure value is logical
    Assertions.assertNotEquals(0, freeDiskSpace);
    assertThat(freeDiskSpace).isGreaterThan(SPACE_ENOUGH_FOR);
  }

  @Test
  @DisplayName("Test used space computation")
  void getUsedSpace() throws IOException {

    File testData = null;
    try {
      testData = createDiskSpaceTestData();
      long actualUsedSpace;
      final long expectedMinUsedSpace = testData.length();
      actualUsedSpace = diskManager.getUsedSpace();
      logger.info(
          "Found {} bytes used in:\n\t{}\\", actualUsedSpace, diskManager.getStorageLocation());

      // tests
      assertThat(actualUsedSpace).isGreaterThanOrEqualTo(expectedMinUsedSpace);
      assertThat(actualUsedSpace).isLessThan(Long.MAX_VALUE);

    } finally {
      if (testData != null) {
        final boolean deleted = testData.delete();
        logger.info("Test successfully deleted test data? " + deleted);
        assertThat(deleted).isTrue();
      }
    }
  }

  @NotNull
  private File createDiskSpaceTestData() throws IOException {

    Path storagePath = diskManager.getStorageLocation().toAbsolutePath();
    final File testDataFile = new File(storagePath.resolve("TEST_FILE.txt").toString());
    final boolean fileCreated = testDataFile.createNewFile();
    logger.info(String.format("Created file %s? %s", testDataFile, fileCreated));

    final String data = "All work and no play makes Jack a dull boy.\n".repeat(TEST_DATA_LINES);
    final OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(testDataFile), StandardCharsets.UTF_8);
    writer.write(data);
    writer.close();
    return testDataFile;
  }
}
