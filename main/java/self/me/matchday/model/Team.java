/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

// Imports
// -----------------------------------------------------
import java.awt.Image;


/**
 * Represents a football team.
 *
 * @author tomas
 */
public class Team 
{
    // Fields
    // -----------------------------------------------------
    private MD5 teamID;
    private String name;
    private String abbreviation;
    private Affinity affinity;
    private int countryID;
    private int prefLanguageID;
    //Images
    private Image emblem;
    private Image background;
}
