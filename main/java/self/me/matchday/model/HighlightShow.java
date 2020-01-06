/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.HighlightShow.HighlightShowId;

/** A highlight show, week in review or other non-Match televised Event. */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "Highlights")
@IdClass(HighlightShowId.class)
public final class HighlightShow extends Event implements Serializable {

  @Id
  @ManyToOne(targetEntity = Competition.class)
  @JoinColumn(name = "competitionId")
  private Competition competition;

  @Id
  @ManyToOne(targetEntity = Season.class)
  @JoinColumn(name = "seasonId")
  private Season season;

  @Id
  @ManyToOne(targetEntity = Fixture.class)
  @JoinColumn(name = "fixtureId")
  private Fixture fixture;

  @Id private String title;

  @Id
  @Column(name = "date")
  private LocalDateTime date;

  /** Default, no arg-constructor */
  HighlightShow() {}

  private HighlightShow(
      Competition competition, Season season, Fixture fixture, String title, LocalDateTime date) {
    this.competition = competition;
    this.season = season;
    this.fixture = fixture;
    this.title = title;
    this.date = date;
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

  /** Class representing the composite primary key for this JPA entity. */
  public static class HighlightShowId implements Serializable {
    protected String competition;
    protected String season;
    protected String fixture;
    protected LocalDateTime date;

    public HighlightShowId() {}

    public HighlightShowId(
        String competitionId, String seasonId, String fixtureId, LocalDateTime date) {
      this.competition = competitionId;
      this.season = seasonId;
      this.fixture = fixtureId;
      this.date = date;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (!(obj instanceof HighlightShowId)) {
        return false;
      }

      // Cast
      final HighlightShowId testId = (HighlightShowId) obj;
      return testId.competition.equals(this.competition)
          && testId.fixture.equals(this.fixture)
          && testId.season.equals(this.season)
          && testId.date.isEqual(this.date);
    }
  }
}
