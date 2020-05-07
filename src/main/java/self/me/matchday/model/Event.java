/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * A sporting Event; could be a Match (game), highlight show, trophy celebration, group selection,
 * or ... ?
 */
@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Event {

  protected static final DateTimeFormatter EVENT_ID_DATE_FORMATTER =
      DateTimeFormatter.ISO_LOCAL_DATE;

  @Id
  protected String eventId;

  @ManyToOne(cascade = {CascadeType.MERGE})
  protected Competition competition;

  @ManyToOne(cascade = CascadeType.MERGE)
  protected Season season;

  @ManyToOne(cascade = CascadeType.MERGE)
  protected Fixture fixture;

  protected String title;
  protected LocalDateTime date;

  public static class EventSorter implements Comparator<Event> {

    @Override
    public int compare(@NotNull Event o1, @NotNull Event o2) {
      return
          o1
              .getDate()
              .compareTo(o2.getDate())
              * -1;
    }
  }
}
