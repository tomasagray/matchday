package self.me.matchday.model;

import java.time.LocalDate;
import java.time.Month;

public class Season {
  // Default parameters
  private static final LocalDate START_DATE = LocalDate.of(-1, Month.AUGUST, 1);
  private static final LocalDate END_DATE = LocalDate.of(-1, Month.MAY, 31);

  // Fields
  private final LocalDate startDate;
  private final LocalDate endDate;

  public Season(int startYear, int endYear) {
    this.startDate = LocalDate.from(START_DATE).withYear(startYear);
    this.endDate = LocalDate.from(END_DATE).withYear(endYear);
  }

  // Getters & Setters
  // -------------------------------------------------------------------------
  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  @Override
  public String toString() {
    return startDate.getYear()
        + " - "
        + endDate.getYear();
  }
}
