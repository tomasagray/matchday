/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.fileserver;

import lombok.Data;

/**
 * Represents a file server user
 */
@Data
public abstract class FSUser {
  // Fields
  private final String userName;
  private String password;
  private boolean keepLoggedIn;

  // Constructors
  public FSUser(String name) {
    this.userName = name;
  }
  public FSUser(String name, String password) {
    this(name);
    this.password = password;
  }
  public FSUser(String name, String password, boolean keepLoggedIn) {
    this(name, password);
    this.keepLoggedIn = keepLoggedIn;
  }

}
