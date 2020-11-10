/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.fileserver;

import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.MD5String;
import self.me.matchday.model.SecureCookie;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a file server user
 */
@Data
@Entity
public class FileServerUser implements Serializable {

  @Id
  private String userId;
  private final String userName;
  private final String password;
  private final String email;
  private boolean loggedIn;
  private String serverId;
  @OneToMany(targetEntity = SecureCookie.class, cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private Collection<SecureCookie> cookies = new ArrayList<>();

  // default constructor
  public FileServerUser() {
    this.userName = this.email = null;
    this.password = null;
  }

  public FileServerUser(@NotNull final String userName, @NotNull final String password) {

    this.userId = MD5String.fromData(userName);
    this.userName = this.email = userName;
    this.password = password;
  }

  /**
   * Set the user to logged in - set boolean, cookies, server ID and object
   * ID for JPA storage
   *
   * @param serverId The ID of the Server this User is logged into
   * @param cookies Any cookies returned with login response
   */
  public void loginToServer(@NotNull final String serverId,
                            @NotNull final Collection<SecureCookie> cookies) {

    setUserId(MD5String.fromData(this.userName, serverId));
    setServerId(serverId);
    setCookies(cookies);
    setLoggedIn(true);
  }

  public void setLoggedOut() {
    // Clear login data
    clearCookies();
    setLoggedIn(false);
  }

  public void setCookies(@NotNull final Collection<SecureCookie> cookies) {
    // ensure empty cookie jar
    clearCookies();
    this.cookies.addAll(cookies);
  }

  public void clearCookies() {
    this.cookies.clear();
  }

  @Override
  public String toString() {
    return
        String.format(
            "Username: %s, Password: *****, Logged in: %s, Email: %s, Server ID: %s, Cookies: %s",
            getUserName(), isLoggedIn(), getEmail(), getServerId(),
            Strings.join(cookies, ','));
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof FileServerUser)) {
      return false;
    }
    // Cast for comparison
    final FileServerUser fileServerUser = (FileServerUser) object;
    return
            this.getUserName().equals(fileServerUser.getUserName()) &&
                    this.getPassword().equals(fileServerUser.getPassword()) &&
                    this.getEmail().equals(fileServerUser.getEmail());

  }
}
