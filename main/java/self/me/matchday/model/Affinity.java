/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

/**
 * Team or competition affinity: how much the user likes/prefers a given team or competition.
 *
 * <p><b>FAVORITE</b>: The Event WILL be downloaded <br/>
 * <b>LIKE</b>: The Event may be downloaded <br/>
 * <b>IGNORE</b>: The Event WILL NOT be downloaded
 *
 * @author tomas
 */
public enum Affinity {
  FAVORITE,
  LIKE,
  IGNORE
}
