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

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.CorrectedOrNull;
import self.me.matchday.model.video.VideoFileSource;

/**
 * Class representing a match (game) between two teams (home & away) in a given Competition on a
 * specific date.
 *
 * @author tomas
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "MatchGame")
public class Match extends Event {

  @CorrectedOrNull
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
  @JoinColumn(nullable = false)
  private Team homeTeam;

  @CorrectedOrNull
  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
  @JoinColumn(nullable = false)
  private Team awayTeam;

  @Builder
  public Match(
      UUID eventId,
      Competition competition,
      Team homeTeam,
      Team awayTeam,
      Season season,
      Fixture fixture,
      LocalDateTime date) {
    super(eventId, competition, season, fixture, date, null);
    this.homeTeam = homeTeam;
    this.awayTeam = awayTeam;
  }

  @NotNull
  @Override
  public String getTitle() {
    return competition.getName().getName()
        + ": "
        + homeTeam.getName().getName()
        + " vs. "
        + awayTeam.getName().getName()
        + ((fixture != null) ? ", " + fixture : "");
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
  public VideoFileSource getFileSource(@NotNull UUID fileSrcId) {
    return super.getFileSource(fileSrcId);
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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Match)) return false;
    if (!super.equals(o)) return false;
    Match match = (Match) o;
    return Objects.equals(getHomeTeam(), match.getHomeTeam())
        && Objects.equals(getAwayTeam(), match.getAwayTeam());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getHomeTeam(), getAwayTeam());
  }
}
