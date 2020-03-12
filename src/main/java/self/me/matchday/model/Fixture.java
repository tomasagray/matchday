/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a specific Fixture within a Season. This object is immutable.
 */
@Data
@Entity
@Table(name = "Fixtures")
public final class Fixture implements Serializable {

  private static final long serialVersionUID = 123456L;   // for cross-platform serialization

  // Default parameters
  private static final String SEPARATOR = " ";
  private static final String DEFAULT_TITLE = "Matchday";
  private static final int DEFAULT_FIXTURE = 0;

  // Fields
  @Id
  private final String fixtureId;
  private String title;
  private Integer fixtureNumber = 0;

  // Default constructor
  public Fixture() {
    this.fixtureId = MD5String.generate();
  }
  public Fixture(@NotNull String title) {
    this.title = title.trim();
    this.fixtureId = MD5String.fromData(title);
  }
  public Fixture(@NotNull String title, int fixtureNumber) {
    this.title = title.trim();
    this.fixtureNumber = fixtureNumber;
    this.fixtureId = MD5String.fromData(title + fixtureNumber);
  }

  @NotNull
  @Contract(pure = true)
  @Override
  public String toString() {
    if(title == null) {
      return "<none>";
    } else {
      return title + " " + ((fixtureNumber != 0) ? ("#" + fixtureNumber) : "");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    }
    if( !(obj instanceof Fixture) ) {
      return false;
    }

    // Cast for comparison
    Fixture fixture = (Fixture)obj;
    return fixture.getFixtureId().equals(this.getFixtureId())
          && fixture.getFixtureNumber().intValue() == this.getFixtureNumber().intValue();
  }

  @Override
  public int hashCode() {
    return Objects.hash(this);
  }
}
