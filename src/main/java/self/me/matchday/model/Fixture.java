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

package self.me.matchday.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Represents a specific Fixture within a Season. This object is immutable. */
@Getter
@Setter
@Embeddable
public final class Fixture implements Serializable {

  private static final String DEFAULT_TITLE = "Matchday";

  private String title;
  private Integer fixtureNumber;

  public Fixture(@NotNull String title) {
    this.title = title.trim();
    this.fixtureNumber = 0;
  }

  public Fixture() {
    this(DEFAULT_TITLE);
  }

  public Fixture(final int fixtureNumber) {
    this.title = DEFAULT_TITLE;
    this.fixtureNumber = fixtureNumber;
  }

  public Fixture(@NotNull String title, int fixtureNumber) {
    this.title = title.trim();
    this.fixtureNumber = fixtureNumber;
  }

  @NotNull
  @Contract(pure = true)
  @Override
  public String toString() {
    final String fixture = fixtureNumber > 0 ? " #" + fixtureNumber : "";
    return title + fixture;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fixture fixture)) return false;
    return Objects.equals(getTitle(), fixture.getTitle())
        && Objects.equals(getFixtureNumber(), fixture.getFixtureNumber());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTitle(), getFixtureNumber());
  }
}
