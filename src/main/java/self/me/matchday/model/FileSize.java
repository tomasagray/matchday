package self.me.matchday.model;

import org.jetbrains.annotations.NotNull;

public class FileSize {

  // Factory methods
  public static @NotNull Long ofGigabytes(final float gigs) {
    // Gigabyte-ify
    return DataSizeUnit.GB.denominate(gigs);
  }
  public static @NotNull Long ofMegabytes(final float megs) {
    // Megabyte-ify
    return DataSizeUnit.MB.denominate(megs);
  }
  public static @NotNull Long ofKilobytes(final float kilobytes) {
    // Kilobyte-ify
    return DataSizeUnit.KB.denominate(kilobytes);
  }
  public static @NotNull Long ofBytes(final long bytes) {
    return bytes;
  }

  public enum DataSizeUnit {

    GB(1024 * 1024 * 1024),
    MB(1024 * 1024),
    KB(1024);

    private final long unitSize;

    DataSizeUnit(final long unitSize) {
      this.unitSize = unitSize;
    }

    public long denominate(final float fileSize) {
      return
          (long) (fileSize * unitSize);
    }
  }
}
