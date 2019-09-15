/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.awt.Image;

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
  private Image background;
}
