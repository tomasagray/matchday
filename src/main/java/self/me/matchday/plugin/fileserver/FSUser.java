/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.fileserver;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a file server user
 */
@Data
@AllArgsConstructor
public class FSUser {

  private final String userName;
  private final String password;
  private final boolean keepLoggedIn;

  @Override
  public String toString() {
    return
        "UserName: "
        + getUserName()
        + "\n"
        + "Password: *****\n"
        + "Keep Logged In: "
        + isKeepLoggedIn();
  }
}
