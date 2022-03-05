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

package self.me.matchday.model;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.Corrected;
import self.me.matchday.CorrectedOrNull;
import self.me.matchday.model.video.VideoFileSource;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A sporting Event */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Event {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(columnDefinition = "BINARY(16)")
  protected UUID eventId;

  @Corrected
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
  protected Competition competition;

  @CorrectedOrNull
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
  protected Team homeTeam;

  @CorrectedOrNull
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
  protected Team awayTeam;

  @Embedded protected Season season;

  @Embedded protected Fixture fixture;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  protected final Set<VideoFileSource> fileSources = new HashSet<>();

  protected LocalDateTime date;

  public String getTitle() {
    return String.format("%s - %s, %s", competition, homeTeam, awayTeam);
  }

  public void addFileSources(@NotNull final Collection<? extends VideoFileSource> fileSources) {
    this.fileSources.addAll(fileSources);
  }

  public void addFileSource(@NotNull final VideoFileSource fileSource) {
    this.fileSources.add(fileSource);
  }

  /**
   * Retrieve a particular file source from this Event
   *
   * @param fileSrcId The ID of the file source
   * @return the requested file source, or null if not found
   */
  public VideoFileSource getFileSource(final @NotNull UUID fileSrcId) {

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
    return Objects.hash(
        eventId, competition, homeTeam, awayTeam, season, fixture, fileSources, date);
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
