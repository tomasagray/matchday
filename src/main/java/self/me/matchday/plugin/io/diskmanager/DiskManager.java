package self.me.matchday.plugin.io.diskmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DiskManager {

  private static final FileSize MIN_FREE_DISK_SPACE = FileSize.ofGigabytes(40);
  private static final FileSize MAX_DISK_CONSUMPTION = FileSize.ofGigabytes(300);

  private final Path storageLocation;
  private final Path fileSystemRoot;

  public DiskManager(@NotNull final DiskManagerProperties properties) {

    // Read storage location from plugin properties
    this.storageLocation = Path.of(properties.getStorageLocation());
    this.fileSystemRoot = determineFileSystemRoot();
  }

  public boolean isSpaceAvailable(@NotNull final FileSize fileSize) throws IOException {

    // Test criteria
    final FileSize totalProposedAllocation = getUsedSpace().add(fileSize);
    final FileSize remainingDiskSpace = getFreeDiskSpace().subtract(fileSize);

    // Perform tests
    final boolean isFreeDiskSpace =
        (remainingDiskSpace.compareTo(MIN_FREE_DISK_SPACE) > 0);
    final boolean isLessThanMax =
        (totalProposedAllocation.compareTo(MAX_DISK_CONSUMPTION) < 0);

    return isFreeDiskSpace && isLessThanMax;
  }

  public FileSize getFreeDiskSpace() {

    // Get a File reference to root
    final File root = fileSystemRoot.toFile();
    final long freeBytes = root.getFreeSpace();
    return FileSize.ofBytes(freeBytes);
  }

  public Path createDirectories(@NotNull final String... dirs) throws IOException {

    return
        Files.createDirectories(Paths.get(storageLocation.toString(), dirs));
  }

  public FileSize getUsedSpace() throws IOException {

    final long size =
        Files
            .walk(storageLocation)
            // find all files
            .filter(path -> path.toFile().isFile())
            // sum file sizes
            .mapToLong(path -> path.toFile().length())
            .sum();
    return FileSize.ofBytes(size);
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

  public static class FileSize implements Comparable<FileSize> {

    @Getter
    private final long bytes;

    private FileSize(final long byteSize) {
      this.bytes = byteSize;
    }

    // Factory methods
    public static @NotNull FileSize ofGigabytes(final int gigs) {
      // Gigabyte-ify
      return new FileSize(DataSizeUnit.GB.denominate(gigs));
    }
    public static @NotNull FileSize ofMegabytes(final int megs) {
      // Megabyte-ify
      return new FileSize(DataSizeUnit.MB.denominate(megs));
    }
    public static @NotNull FileSize ofKilobytes(final int kilobytes) {
      // Kilobyte-ify
      return new FileSize(DataSizeUnit.KB.denominate(kilobytes));
    }
    public static @NotNull FileSize ofBytes(final long bytes) {
      return new FileSize(bytes);
    }

    public FileSize add(@NotNull final FileSize fileSize) {
      return
          FileSize.ofBytes(fileSize.getBytes() + this.getBytes());
    }

    public FileSize subtract(@NotNull final FileSize fileSize) {
      return
          FileSize.ofBytes(this.getBytes() - fileSize.getBytes());
    }

    @Override
    public int compareTo(@NotNull FileSize compare) {
      return (int)(this.bytes - compare.bytes);
    }

    public enum DataSizeUnit {

      GB(1024 * 1024 * 1024),
      MB(1024 * 1024),
      KB(1024);

      private final long unitSize;

      DataSizeUnit(final long unitSize) {
        this.unitSize = unitSize;
      }

      public long denominate(final long fileSize) {
        return
            fileSize * unitSize;
      }
    }
  }

}
