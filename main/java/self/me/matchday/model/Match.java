/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.awt.Image;
import java.security.Timestamp;

/**
 * Class representing a match (game) between two teams (home & away)
 * in a given Competition on a specific date.
 *
 * @author tomas
 */
public class Match 
{
    // Fields
    // -----------------------------------------------------
    private MD5 matchID;
    // Match components
    private Team homeTeam;
    private Team awayTeam;
    private Competition competition;
    private Timestamp timestamp;
    private Season season;
    // Cover art
    private Image artwork;
}
