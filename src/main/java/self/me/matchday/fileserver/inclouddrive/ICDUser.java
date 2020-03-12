/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver.inclouddrive;

import self.me.matchday.fileserver.FSUser;

/**
 * Represents a file server user at InCloudDrive.com
 */
public class ICDUser extends FSUser {
  private static final String LOG_TAG = "ICDUser";

  public ICDUser(String name) {
    super(name);
  }

  public ICDUser(String name, String password) {
    super(name, password);
  }

  public ICDUser(String name, String password, boolean keepLoggedIn) {
    super(name, password, keepLoggedIn);
  }

  @Override
  public String toString() {
    return "UserName: "
        + getUserName()
        + "\n"
        + "Password: *****\n"
        + "Keep Logged In: "
        + isKeepLoggedIn();
  }
}
