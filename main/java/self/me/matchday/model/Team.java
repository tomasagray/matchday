/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

// Imports
// -----------------------------------------------------
import java.awt.Image;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a football team.
 *
 * @author tomas
 */
public class Team {
  // Fields
  // -----------------------------------------------------
  private MD5 teamID;
  private String name;
  private String abbreviation;
  private Affinity affinity;
  private int countryID;
  private int prefLanguageID;
  // Images
  private Image emblem;
  private Image background;

  public Team(@NotNull String name) {
    this.name = name.trim();
    // Defaults
    this.abbreviation = this.name.substring(0,2);
    this.affinity = Affinity.IGNORE;
    this.teamID = new MD5(name);
  }

  public MD5 getTeamID() {
    return teamID;
  }

  public String getName() {
    return name;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  public Affinity getAffinity() {
    return affinity;
  }

  public void setAbbreviation(String abbreviation) {
    this.abbreviation = abbreviation;
  }

  public void setAffinity(Affinity affinity) {
    this.affinity = affinity;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
