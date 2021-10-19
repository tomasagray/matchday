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

package self.me.matchday.model;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/** Represents a specific Fixture within a Season. This object is immutable. */
@Getter
@Setter
@Entity
@Table(name = "Fixtures")
public final class Fixture implements Serializable {

  private static final long serialVersionUID = 123456L;

  // Default parameters
  private static final String SEPARATOR = " ";
  private static final String DEFAULT_TITLE = "Matchday";
  private static final int DEFAULT_FIXTURE = 0;

  // Fields
  @Id private final String fixtureId;
  private String title;
  private Integer fixtureNumber = 0;

  public Fixture() {
    this.fixtureId = MD5String.generate();
  }

  public Fixture(@NotNull String title) {
    this.title = title.trim();
    this.fixtureId = MD5String.fromData(title);
  }

  public Fixture(final int fixtureNumber) {
    this.title = DEFAULT_TITLE;
    this.fixtureNumber = fixtureNumber;
    this.fixtureId = MD5String.fromData(title, fixtureNumber);
  }

  public Fixture(@NotNull String title, int fixtureNumber) {
    this.title = title.trim();
    this.fixtureNumber = fixtureNumber;
    this.fixtureId = MD5String.fromData(title, fixtureNumber);
  }

  @NotNull
  @Contract(pure = true)
  @Override
  public String toString() {
    if (title == null) {
      return "<none>";
    } else {
      return title + " " + ((fixtureNumber != 0) ? ("#" + fixtureNumber) : "");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Fixture)) {
      return false;
    }

    // Cast for comparison
    Fixture fixture = (Fixture) obj;
    return fixture.getFixtureId().equals(this.getFixtureId())
        && fixture.getFixtureNumber().intValue() == this.getFixtureNumber().intValue();
  }

  @Override
  public int hashCode() {
    int factor = (fixtureNumber == 0) ? 1 : fixtureNumber;
    return fixtureId.hashCode() * title.hashCode() * factor;
  }
}
