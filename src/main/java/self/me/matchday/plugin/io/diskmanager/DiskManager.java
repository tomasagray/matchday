package self.me.matchday.plugin.io.diskmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.FileSize;

@Component
public class DiskManager {

  private static final Long MIN_FREE_DISK_SPACE = FileSize.ofGigabytes(40);
  private static final Long MAX_DISK_CONSUMPTION = FileSize.ofGigabytes(300);

  private final Path storageLocation;
  private final Path fileSystemRoot;

  public DiskManager(@NotNull final DiskManagerProperties properties) {

    // Read storage location from plugin properties
    this.storageLocation = Path.of(properties.getStorageLocation());
    this.fileSystemRoot = determineFileSystemRoot();
  }

  public boolean isSpaceAvailable(@NotNull final Long fileSize) throws IOException {

    // Test criteria
    final Long totalProposedAllocation = getUsedSpace() + fileSize;
    final Long remainingDiskSpace = getFreeDiskSpace() - fileSize;

    // Perform tests
    final boolean isFreeDiskSpace =
        (remainingDiskSpace.compareTo(MIN_FREE_DISK_SPACE) > 0);
    final boolean isLessThanMax =
        (totalProposedAllocation.compareTo(MAX_DISK_CONSUMPTION) < 0);

    return isFreeDiskSpace && isLessThanMax;
  }

  public Long getFreeDiskSpace() {

    // Get a File reference to root
    final File root = fileSystemRoot.toFile();
    return root.getFreeSpace();
  }

  public Path createDirectories(@NotNull final String... dirs) throws IOException {

    return
        Files.createDirectories(Paths.get(storageLocation.toString(), dirs));
  }

  public Long getUsedSpace() throws IOException {

    return
        Files
            .walk(storageLocation)
            // find all files
            .filter(path -> path.toFile().isFile())
            // sum file sizes
            .mapToLong(path -> path.toFile().length())
            .sum();
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
