/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.fileserver;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.MD5String;
import self.me.matchday.model.SecureCookie;

/**
 * Represents a file server user
 */
@Data
@Entity
public class FileServerUser {

  @Id
  private String userId;
  private final String userName;
  private final String password;
  private String email;
  private boolean loggedIn;
  private String serverId;
  @ElementCollection
  @OneToMany(targetEntity = SecureCookie.class, cascade = CascadeType.ALL, orphanRemoval = true)
  private Collection<SecureCookie> cookies = new ArrayList<>();

  // default constructor
  public FileServerUser() {
    this.userName = null;
    this.password = null;
  }

  public FileServerUser(@NotNull final String userName, @NotNull final String password) {

    this.userId = MD5String.fromData(userName);
    this.userName = userName;
    this.password = password;
  }

  /**
   * Set the user to logged in - set boolean, cookies, server ID and object
   * ID for JPA storage
   *
   * @param serverId The ID of the Server this User is logged into
   * @param cookies Any cookies returned with login response
   */
  public void setLoggedIn(@NotNull final String serverId,
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
    this.cookies.addAll(cookies);
  }

  public void clearCookies() {
    this.cookies.clear();
  }

  private void setLoggedIn(boolean loggedIn) {
    this.loggedIn = loggedIn;
  }

  @Override
  public String toString() {
    return
        String.format(
            "Username: %s, Password: *****, Logged in: %s, Email: %s, Server ID: %s, Cookies: %s",
            getUserName(), isLoggedIn(), getEmail(), getServerId(),
            Strings.join(cookies, ','));
  }
}
