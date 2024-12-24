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

package net.tomasbot.matchday.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Represents a specific Fixture within a Season. This object is immutable. */
@Getter
@Embeddable
public final class Fixture implements Serializable, Comparable<Fixture> {

  private static final String DEFAULT_TITLE = "Matchday";

  public static final Fixture GroupStage = new Fixture("Group Stage", 256);
  public static final Fixture RoundOf64 = new Fixture("Round of 64", 1_024 * 16);
  public static final Fixture RoundOf32 = new Fixture("Round of 32", 1_024 * 32);
  public static final Fixture RoundOf16 = new Fixture("Round of 16", 1_024 * 64);
  public static final Fixture QuarterFinal = new Fixture("Quarter-Final", 1_024 * 1_024);
  public static final Fixture SemiFinal = new Fixture("Semi-Final", 1_024 * 1_024 * 4);
  public static final Fixture Playoff = new Fixture("Playoff", 1_024 * 1_024 * 4 * 2);
  public static final Fixture Final = new Fixture("Final", 1_024 * 1_024 * 4 * 4);

  private final String title;
  private final Integer fixtureNumber;

  public Fixture(@NotNull String title) {
    this.title = title.trim();
    this.fixtureNumber = 0;
  }

  public Fixture() {
    this("");
  }

  public Fixture(int fixtureNumber) {
    this.title = String.format("%s %d", DEFAULT_TITLE, fixtureNumber);
    this.fixtureNumber = fixtureNumber;
  }

  public Fixture(@NotNull String title, int fixtureNumber) {
    this.title = title.trim();
    this.fixtureNumber = fixtureNumber;
  }

  public Fixture(@NotNull Fixture fixture) {
    this.title = fixture.getTitle();
    this.fixtureNumber = fixture.getFixtureNumber();
  }

  @Override
  public String toString() {
    return this.title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fixture fixture)) return false;
    return this.getFixtureNumber().equals(fixture.getFixtureNumber());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTitle(), getFixtureNumber());
  }

  @Override
  public int compareTo(@NotNull Fixture fixture) {
    return this.getFixtureNumber().compareTo(fixture.getFixtureNumber());
  }
}
