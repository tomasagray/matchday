/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday;

import org.jetbrains.annotations.Contract;

/**
 * Singleton class responsible for handling application settings.
 * @author tomas
 */
public class Settings {
    private static Settings INSTANCE;

    @Contract(pure = true)
    private Settings() {}

    public static Settings getInstance() {
      if(INSTANCE == null) {
        INSTANCE = new Settings();
      }

      return INSTANCE;
    }
}
