/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

/**
 * Team or competition affinity: how much the user likes/prefers a given team or competition.
 *
 * <p>FAVORITE: The match WILL be downloaded LIKE: The match may be downloaded, depending on total
 * score of both teams + competition IGNORE: The match WILL NOT be downloaded
 *
 * @author tomas
 */
public enum Affinity {
  FAVORITE,
  LIKE,
  IGNORE
}
