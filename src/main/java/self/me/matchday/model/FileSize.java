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
