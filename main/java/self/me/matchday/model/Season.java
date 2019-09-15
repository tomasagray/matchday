package self.me.matchday.model;

import java.time.LocalDate;
import java.time.ZoneId;

public class Season {
  private LocalDate startDate;
  private LocalDate endDate;
  private ZoneId timeZone;

  public Season(LocalDate startDate, LocalDate endDate, ZoneId timeZone) {
    this.startDate = startDate;
    this.endDate = endDate;
    this.timeZone = timeZone;
  }

  // Getters & Setters
  // -------------------------------------------------------------------------
  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }
}
