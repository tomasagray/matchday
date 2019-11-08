/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.awt.Image;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a competition, e.g., a domestic league (EPL) or cup (FA Cup), a tournament (UCL, World
 * Cup), etc.
 *
 * @author tomas
 */
public class Competition {
  // Fields
  // -----------------------------------------------------
  private MD5 competitionID;
  private String name;
  private String abbreviation;
  private Affinity affinity;
  private int countryID;
  // Images
  private Image emblem;
  private Image clearLogo;
  private Image landscape;

  // TODO: Fix this
  public Competition(@NotNull String name) {
    this.name = name;
    // Automatically create an abbreviation if none is supplied
    this.abbreviation = this.name.substring(0, 2);
    // Generate ID
    this.competitionID = new MD5(name);
  }

  public Competition(@NotNull String name, @NotNull String abbreviation, @NotNull MD5 competitionID) {
    this.name = name;
    this.abbreviation = abbreviation;
    this.competitionID = competitionID;
  }

  public String getName() {
    return name;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
