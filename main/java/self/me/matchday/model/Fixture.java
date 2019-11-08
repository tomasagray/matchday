package self.me.matchday.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a specific Fixture within a Season. This object is immutable.
 */
public final class Fixture {
  private static final String SEPARATOR = " ";
  private static final String DEFAULT_TITLE = "Matchday";
  private static final int DEFAULT_FIXTURE = 0;

  private String title;
  private int fixtureNumber;

  /**
   * Takes a formatted String and parses out a title and fixture number.
   *
   * @param title Expected formatted String: <title> <number>
   */
  public Fixture(@NotNull String title) {
    this.title = title;
  }

  public Fixture(@NotNull String title, int fixtureNumber) {
    this.title = title.trim();
    this.fixtureNumber = fixtureNumber;
  }

  public String getTitle() {
    return title;
  }

  public int getFixtureNumber() {
    return fixtureNumber;
  }

  public void setFixtureNumber(int fixtureNumber) {
    this.fixtureNumber = fixtureNumber;
  }

  @NotNull
  @Contract(pure = true)
  @Override
  public String toString() {
    return title + " " + ((fixtureNumber != 0) ? fixtureNumber : "");
  }
}
