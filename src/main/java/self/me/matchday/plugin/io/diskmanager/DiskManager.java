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

package self.me.matchday.plugin.io.diskmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.SettingsService;
import self.me.matchday.model.FileSize;

@Component
public class DiskManager {

  private static final Long MIN_FREE_DISK_SPACE = FileSize.ofGigabytes(40);
  private static final Long MAX_DISK_CONSUMPTION = FileSize.ofGigabytes(300);

  @Getter private final Path storageLocation;
  @Getter private final Path fileSystemRoot;

  public DiskManager(@NotNull SettingsService settingsService) {
    this.storageLocation = settingsService.getSettings().getVideoStorageLocation();
    this.fileSystemRoot = determineFileSystemRoot();
  }

  public boolean isSpaceAvailable(@NotNull final Long fileSize) throws IOException {

    // Test criteria
    final Long totalProposedAllocation = getUsedSpace() + fileSize;
    final Long remainingDiskSpace = getFreeDiskSpace() - fileSize;

    // Perform tests
    final boolean isFreeDiskSpace = (remainingDiskSpace.compareTo(MIN_FREE_DISK_SPACE) > 0);
    final boolean isLessThanMax = (totalProposedAllocation.compareTo(MAX_DISK_CONSUMPTION) < 0);

    return isFreeDiskSpace && isLessThanMax;
  }

  public Long getFreeDiskSpace() {

    // Get a File reference to root
    final File root = fileSystemRoot.toFile();
    return root.getFreeSpace();
  }

  public Long getUsedSpace() throws IOException {

    try (Stream<Path> walker = Files.walk(storageLocation)) {
      return walker
          .filter(path -> path.toFile().isFile()) // find all files
          .mapToLong(path -> path.toFile().length()) // sum file sizes
          .sum();
    }
  }

  private Path determineFileSystemRoot() {

    // Copy reference to initial storage dir
    Path parent = storageLocation;
    // Recursively search for parents
    while (parent.getParent() != null) {
      parent = parent.getParent();
    }
    return parent;
  }
}
