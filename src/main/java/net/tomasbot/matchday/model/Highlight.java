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

package net.tomasbot.matchday.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.model.video.VideoFileSource;

/** A highlight show, week in review or other non-Match televised Event. */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Highlight extends Event implements Serializable {

  @Builder(builderMethodName = "highlightBuilder")
  public Highlight(
      UUID eventId, Competition competition, Season season, Fixture fixture, LocalDateTime date) {
    super(eventId, competition, season, fixture, date, null);
  }

  @Override
  public String getTitle() {
    return String.format(
        "%s Highlights, %s",
        getCompetition().getName().getName(), getDate().format(DateTimeFormatter.ISO_DATE));
  }

  // --------------------------------------
  // Next 8 methods included for reflection
  @Override
  public UUID getEventId() {
    return super.getEventId();
  }

  @Override
  public Competition getCompetition() {
    return super.getCompetition();
  }

  @Override
  public void setCompetition(Competition competition) {
    super.setCompetition(competition);
  }

  @Override
  public Season getSeason() {
    return super.getSeason();
  }

  @Override
  public Fixture getFixture() {
    return super.getFixture();
  }

  @Override
  public Set<VideoFileSource> getFileSources() {
    return super.getFileSources();
  }

  @Override
  public LocalDateTime getDate() {
    return super.getDate();
  }

  @Override
  public Artwork getArtwork() {
    return super.getArtwork();
  }

  // End redundant reflection overrides
  // ----------------------------------

  @Override
  public @NotNull String toString() {
    return String.format(
        "Highlight{eventId=%s, competition=%s, season=%s, fixture=%s, date=%s}",
        eventId, competition, season, fixture, date);
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final Highlight highlight)) {
      return false;
    }
    // Cast for comparison
    return this.getEventId().equals(highlight.getEventId())
        && this.getCompetition().equals(highlight.getCompetition())
        && this.getFixture().equals(highlight.getFixture())
        && this.getSeason().equals(highlight.getSeason());
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + eventId.hashCode();
    hash = 31 * hash + ((competition != null) ? competition.hashCode() : 0);
    hash = 31 * hash + ((fixture != null) ? fixture.hashCode() : 0);
    hash = 31 * hash + ((season != null) ? season.hashCode() : 0);
    return hash;
  }
}
