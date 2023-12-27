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

package self.me.matchday.model.video;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/** Football match part identifiers */
public enum PartIdentifier {
  DEFAULT("<>"),
  PRE_MATCH("Pre-Match"),
  FIRST_HALF("1st Half"),
  SECOND_HALF("2nd Half"),
  EXTRA_TIME("Extra-Time/Penalties"),
  TROPHY_CEREMONY("Trophy Ceremony"),
  POST_MATCH("Post-Match"),
  FULL_COVERAGE("Full Coverage");

  private final String partName;

  PartIdentifier(@NotNull String partName) {
    this.partName = partName;
  }

  public String getPartName() {
    return partName;
  }

  @Override
  public String toString() {
    return getPartName();
  }

  public static PartIdentifier from(@NotNull String value) {
    try {
      return PartIdentifier.valueOf(value);
    } catch (IllegalArgumentException e) {
      return Arrays.stream(PartIdentifier.values())
          .filter(name -> name.getPartName().equals(value))
          .findAny()
          .orElseThrow(() -> e);
    }
  }
}
