/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

/** A highlight show, week in review or other non-Match televised Event. */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "Highlights")
public class HighlightShow extends Event implements Serializable {

  // For external identification (MD5 String)
  private final String highlightShowId;

  /** Default, no arg-constructor */
  HighlightShow() {
    this.highlightShowId = MD5String.generate();
  }

  private HighlightShow(
      Competition competition, Season season, Fixture fixture, String title, LocalDateTime date) {
    this.competition = competition;
    this.season = season;
    this.fixture = fixture;
    this.title = title;
    this.date = date;
    this.highlightShowId = generateHighlightShowId();
  }

  // Overrides
  @NotNull
  @Override
  public String toString() {
    return getTitle()
        + " ("
        + getCompetition()
        + " "
        + getSeason().getStartDate().getYear()
        + "/"
        + getSeason().getEndDate().getYear()
        + "), "
        + getFixture();
  }

  /** A Builder class for this object. Returns a fully constructed HighlightShow object. */
  public static class HighlightShowBuilder {

    private Competition competition;
    private Season season;
    private Fixture fixture;
    private String title;
    private LocalDateTime date;

    public HighlightShowBuilder setCompetition(@NotNull Competition competition) {
      this.competition = competition;
      return this;
    }

    public HighlightShowBuilder setSeason(@NotNull Season season) {
      this.season = season;
      return this;
    }

    public HighlightShowBuilder setFixture(@NotNull Fixture fixture) {
      this.fixture = fixture;
      return this;
    }

    public HighlightShowBuilder setTitle(@NotNull String title) {
      this.title = title;
      return this;
    }

    public HighlightShowBuilder setDate(LocalDateTime date) {
      this.date = date;
      return this;
    }

    public HighlightShow build() {
      return new HighlightShow(this.competition, this.season, this.fixture, this.title, this.date);
    }
  }

  private String generateHighlightShowId() {

    return MD5String.fromData(
        this.title
            + this.getCompetition().getCompetitionId()
            + this.getSeason().getSeasonId()
            + this.getFixture().getFixtureId()
            + this.getDate().format(EVENT_ID_DATE_FORMATTER));
  }
}
