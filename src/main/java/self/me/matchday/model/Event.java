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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.Corrected;
import self.me.matchday.model.video.VideoFileSource;

/** A sporting Event */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Event {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(columnDefinition = "BINARY(16)")
  protected UUID eventId;

  @Corrected
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
  protected Competition competition;

  @Embedded protected Season season = new Season();

  @Embedded protected Fixture fixture = new Fixture();

  @Setter(AccessLevel.NONE)
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
  protected final Set<VideoFileSource> fileSources = new HashSet<>();

  protected LocalDateTime date;

  @OneToOne(cascade = CascadeType.ALL)
  protected Artwork artwork;

  public void addAllFileSources(@NotNull final Collection<? extends VideoFileSource> fileSources) {
    fileSources.forEach(this::addFileSource);
  }

  public void addFileSource(@NotNull final VideoFileSource fileSource) {
    // replace existing with updated
    for (VideoFileSource source : this.fileSources) {
      if (source.equals(fileSource)) {
        source.addAllVideoFilePacks(fileSource.getVideoFilePacks());
        return;
      }
    }
    // else...
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

  public abstract String getTitle();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Event)) return false;
    Event event = (Event) o;
    return Objects.equals(getCompetition(), event.getCompetition())
        && Objects.equals(getSeason(), event.getSeason())
        && Objects.equals(getFixture(), event.getFixture())
        && Objects.equals(getDate(), event.getDate());
  }

  @Override
  public int hashCode() {
    return Objects.hash(competition, season, fixture, fileSources, date);
  }

  @Override
  public String toString() {
    return String.format(
        "Event{eventId=%s, competition=%s, season=%s, fixture=%s, date=%s}",
        eventId, competition, season, fixture, date);
  }

  /** Defines default Event sorting order - reverse chronological. */
  public static class EventSorter implements Comparator<Event> {

    private static final ZoneOffset OFFSET = ZoneOffset.UTC;
    private static final int REVERSE = -1;

    @Override
    public int compare(@NotNull Event o1, @NotNull Event o2) {
      long r1 = Integer.MAX_VALUE, r2 = Integer.MAX_VALUE;
      final LocalDateTime date1 = o1.getDate();
      final LocalDateTime date2 = o2.getDate();
      if (date1 != null) {
        r1 = date1.toEpochSecond(OFFSET);
      }
      if (date2 != null) {
        r2 = date2.toEpochSecond(OFFSET);
      }
      return Long.compare(r1, r2) * REVERSE;
    }
  }
}
