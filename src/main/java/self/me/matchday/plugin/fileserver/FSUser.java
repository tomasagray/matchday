/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.fileserver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a file server user
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FSUser {

  private String userName;    // todo - make final?
  private String password;
  private boolean keepLoggedIn;

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
