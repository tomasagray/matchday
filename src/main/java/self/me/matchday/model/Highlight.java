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
public class Highlight extends Event implements Serializable {

  Highlight() {
    this.eventId = MD5String.generate();
  }

  private Highlight(
      Competition competition, Season season, Fixture fixture, String title, LocalDateTime date) {
    this.competition = competition;
    this.season = season;
    this.fixture = fixture;
    this.title = title;
    this.date = date;
    this.eventId = generateHighlightId();
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

  /** A Builder class for this object. Returns a fully constructed Highlight object. */
  public static class HighlightBuilder {

    private Competition competition;
    private Season season;
    private Fixture fixture;
    private String title;
    private LocalDateTime date;

    public HighlightBuilder setCompetition(@NotNull Competition competition) {
      this.competition = competition;
      return this;
    }

    public HighlightBuilder setSeason(@NotNull Season season) {
      this.season = season;
      return this;
    }

    public HighlightBuilder setFixture(@NotNull Fixture fixture) {
      this.fixture = fixture;
      return this;
    }

    public HighlightBuilder setTitle(@NotNull String title) {
      this.title = title;
      return this;
    }

    public HighlightBuilder setDate(LocalDateTime date) {
      this.date = date;
      return this;
    }

    public Highlight build() {
      return new Highlight(this.competition, this.season, this.fixture, this.title, this.date);
    }
  }

  private String generateHighlightId() {

    final String dateString =
        (this.getDate() != null) ?
            this.getDate().format(EVENT_ID_DATE_FORMATTER) :
            "NULL";
    return MD5String.fromData(
        this.getTitle()
            + this.getCompetition()
            + this.getSeason()
            + this.getFixture()
            + dateString);
  }
}
