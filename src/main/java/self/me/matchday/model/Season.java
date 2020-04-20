/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@Data
@Entity
@Table(name = "Seasons")
@Embeddable
public final class Season implements Serializable {

  private static final String LOG_TAG = "SeasonClass";
  private static final long serialVersionUID = 123456L;   // for cross-platform serialization

  // Default parameters
  private static final int MILLENNIUM = 2_000;
  private static final int MAX_YEAR = 3_000;
  private static final int MIN_YEAR = 1_900;
  private static final int DEFAULT_YEAR = -1;
  private static final int DEFAULT_START_DAY = 1;
  private static final int DEFAULT_END_DAY = 31;
  private static final LocalDate START_DATE = LocalDate
      .of(DEFAULT_YEAR, Month.AUGUST, DEFAULT_START_DAY);
  private static final LocalDate END_DATE = LocalDate.of(DEFAULT_YEAR, Month.MAY, DEFAULT_END_DAY);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

  // Fields
  @Id
  private final String seasonId;
  private final LocalDate startDate;
  private final LocalDate endDate;

  /**
   * Default constructor; defaults to the current year, Aug 1 to May 31 of next year.
   */
  public Season() {
    this.startDate = LocalDate.from(START_DATE).withYear(LocalDate.now().getYear());
    this.endDate = this.startDate.plusYears(1);
    this.seasonId = MD5String.fromData(this.startDate.toString());
  }

  /**
   * Create a new Season object. Defaults to Aug 1, and May 31 of given years. Two-digit years
   * (e.g., 14) are changed to four-digit by adding the millennium (2000). Illegal dates throw a
   * DateTimeFormatException.
   *
   * @param startYear The beginning year of the season (ex.: 2019)
   * @param endYear   The end year of the season (ex: 2020)
   */
  public Season(final int startYear, final int endYear) {
    // Check for illegal dates
    if (startYear < MIN_YEAR || startYear > MAX_YEAR
        || endYear < MIN_YEAR || endYear > MAX_YEAR) {

      String msg =
          String.format("The years must be within the range: 2000 - 3000; given: %s, %s",
              startYear, endYear);
      String data = String.format("%s/%s", startYear, endYear);
      throw new DateTimeParseException(msg, data, 0);
    }

    this.startDate = LocalDate.from(START_DATE).withYear(startYear);
    this.endDate = LocalDate.from(END_DATE).withYear(endYear);
    this.seasonId = MD5String.fromData(this.startDate.toString());
  }

  @NotNull
  @Contract(pure = true)
  @Override
  public String toString() {
    return DATE_FORMATTER.format(startDate) + " - " + DATE_FORMATTER.format(endDate);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Season)) {
      return false;
    }

    // Cast for comparison
    Season season = (Season) obj;
    return season.getStartDate().isEqual(this.getStartDate())
        && season.getEndDate().isEqual(this.getEndDate());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this);
  }
}
