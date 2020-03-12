/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

// Imports

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Abbreviator;

/**
 * Represents a football team.
 *
 * @author tomas
 */
@Data
@Entity
@Table(name = "Teams")
public class Team implements Serializable {

  private static final long serialVersionUID = 123456L; // for serialization across platforms

  // Fields
  @Id
  @GeneratedValue
//  @Column(name = "teamId")
  private Long teamId;

  private String name;
  private String abbreviation;
  private Locale locale;

  // Default constructor
  public Team() {
    /*this.teamId = MD5String.generate();*/
  }

  public Team(@NotNull String name) {
    this.name = name;
    // Defaults
    this.abbreviation = Abbreviator.abbreviate(this.name);
//    this.teamId = MD5String.fromData(name);
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this);
  }

  /**
   * Compare Teams; they must have identical names and Locales.
   *
   * @param obj The team to be compared
   * @return True/false.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof Team)) {
      return false;
    }

    // Cast for comparison
    final Team team = (Team) obj;
    return team.getName().equals(this.getName()) && team.getLocale().equals(this.getLocale());
  }
}
