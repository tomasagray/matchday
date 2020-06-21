/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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

  @Id
  protected String eventId;
  @ManyToOne(cascade = CascadeType.MERGE)
  protected Competition competition;
  @ManyToOne(cascade = CascadeType.MERGE)
  protected Season season;
  @ManyToOne(cascade = CascadeType.MERGE)
  protected Fixture fixture;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  protected final Set<EventFileSource> fileSources = new TreeSet<>();
  protected String title;
  protected LocalDateTime date;

  /**
   * Encapsulates the file source TreeSet<> logic.
   *
   * @param fileSources A Collection<> of EventFileSources
   * @return True/false if the collection was modified
   */
  public boolean addFileSources(@NotNull final Collection<EventFileSource> fileSources) {
    // Add all collection elements to set
    return this.fileSources.addAll(fileSources);
  }

  // Ensure consistent Event ID generation
  protected static final DateTimeFormatter EVENT_ID_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-W");

  /**
   * Defines default Event sorting order - reverse chronological.
   */
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
