package self.me.matchday.feed;

import java.time.LocalDate;
import self.me.matchday.model.Event;

public interface IEventParser {

  Event getEvent();

  /**
   * Correct year to YYYY format
   *
   * @param year The year to be fixed
   * @return The year in YYYY format
   */
  default int fixYear(final int year) {

    // Constants
    final int MILLENNIUM = 2_000;
    final int CENTURY = 100;
    final int CURRENT_YEAR = LocalDate.now().getYear() % CENTURY;

    if (year < CENTURY) {
      if (year <= CURRENT_YEAR) {
        // 20xx
        return year + MILLENNIUM;
      } else {
        // 19xx
        return year + MILLENNIUM - CENTURY;
      }
    }
    // No changes necessary
    return year;
  }
}
