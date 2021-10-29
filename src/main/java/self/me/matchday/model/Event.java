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

import lombok.*;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.Corrected;
import self.me.matchday.CorrectedOrNull;
import self.me.matchday.model.video.VideoFileSource;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * A sporting Event; could be a Match (game), highlight show, trophy celebration, group selection,
 * or ... ?
 */
@Getter
@Setter
@ToString
// @RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Event {

  @Id protected String eventId;

  @Corrected
  @ManyToOne(cascade = CascadeType.MERGE)
  protected Competition competition;

  @CorrectedOrNull
  @ManyToOne(cascade = CascadeType.MERGE)
  protected Team homeTeam;

  @CorrectedOrNull
  @ManyToOne(cascade = CascadeType.MERGE)
  protected Team awayTeam;

  @ManyToOne(cascade = CascadeType.MERGE)
  protected Season season;

  @ManyToOne(cascade = CascadeType.MERGE)
  protected Fixture fixture;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  protected final Set<VideoFileSource> fileSources = new HashSet<>();

  protected String title;
  protected LocalDateTime date;

  /**
   * Encapsulates the file source TreeSet<> logic.
   *
   * @param fileSources A Collection<> of VideoFileSources
   */
  public void addFileSources(@NotNull final Collection<VideoFileSource> fileSources) {
    // Add all collection elements to set
    this.fileSources.addAll(fileSources);
  }

  /**
   * Retrieve a particular file source from this Event
   *
   * @param fileSrcId The ID of the file source
   * @return the requested file source, or null if not found
   */
  public VideoFileSource getFileSource(final @NotNull String fileSrcId) {

    // Search the collection of file sources for the ID
    for (VideoFileSource fileSrc : fileSources) {
      if (fileSrc.getFileSrcId().equals(fileSrcId)) {
        return fileSrc;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof Event)) {
      return false;
    }

    // Cast for comparison
    final Event event = (Event) o;
    return this.getEventId().equals(event.getEventId()) && this.getTitle().equals(event.getTitle());
  }

  @Override
  public int hashCode() {
    int hash = 11;
    hash = 31 * hash + eventId.hashCode();
    hash = 31 * hash + ((title != null) ? title.hashCode() : 0);
    return hash;
  }

  // Ensure consistent Event ID generation
  protected static final DateTimeFormatter EVENT_ID_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-W");

  /** Defines default Event sorting order - reverse chronological. */
  public static class EventSorter implements Comparator<Event> {

    @Override
    public int compare(@NotNull Event o1, @NotNull Event o2) {
      return o1.getDate().compareTo(o2.getDate()) * -1;
    }
  }
}
