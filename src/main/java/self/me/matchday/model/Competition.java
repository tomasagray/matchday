/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.io.Serializable;
import java.util.Locale;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Abbreviator;

/**
 * Represents a competition, e.g., a domestic league (EPL) or cup (FA Cup), a tournament (UCL, World
 * Cup), etc.
 *
 * @author tomas
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "Competitions")
public class Competition implements Serializable {

  private static final long serialVersionUID = 123456L;   // for cross-platform serialization

  // Fields
  @Id
  private String competitionId;
  private String name;
  private String abbreviation;
  private Locale locale;
  @OneToOne
  private Artwork emblem;
  @OneToOne
  private Artwork fanart;
  @OneToOne
  private Artwork monochromeEmblem;
  @OneToOne
  private Artwork landscape;

  public Competition(@NotNull final String name) {
    // Automatically create an abbreviation
    this(name, Abbreviator.abbreviate(name));
  }

  public Competition(@NotNull final String name, @NotNull final String abbreviation) {

    this.name = name;
    this.abbreviation = abbreviation;
    // Generate ID
    this.competitionId = MD5String.fromData(this.name, this.abbreviation);
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    }
    if( !(obj instanceof Competition) ) {
      return false;
    }

    // Cast for comparison
    Competition competition = (Competition)obj;
    return competition.getName().equals(this.getName());
  }

  @Override
  public int hashCode() {
    return name.hashCode() * competitionId.hashCode();
  }
}
