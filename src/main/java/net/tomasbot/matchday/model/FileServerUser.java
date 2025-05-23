/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

/** Represents a file server user */
@Getter
@RequiredArgsConstructor
@Entity
public final class FileServerUser implements Serializable {

  @Column(columnDefinition = "TEXT")
  private final String username;

  @Column(columnDefinition = "TEXT")
  private final String password;

  @Column(columnDefinition = "TEXT")
  private final String email;

  @OneToMany(
      targetEntity = SecureCookie.class,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private final Collection<SecureCookie> cookies = new ArrayList<>();

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  @Setter
  private UUID userId;

  private boolean loggedIn;

  @Type(type = "uuid-char")
  private UUID serverId;

  public FileServerUser() {
    this.username = this.email = null;
    this.password = null;
  }

  public FileServerUser(
      @NotNull final String username,
      @NotNull final String password,
      @NotNull final UUID serverId) {
    this.username = this.email = username;
    this.password = password;
    this.serverId = serverId;
  }

  /**
   * Set the user to logged in - set boolean, cookies, server ID and object ID for JPA storage
   *
   * @param serverId The ID of the Server this User is logged into
   * @param cookies Any cookies returned with login response
   */
  public void setLoggedIntoServer(
      @NotNull final UUID serverId, @NotNull final Collection<SecureCookie> cookies) {
    this.serverId = serverId;
    this.loggedIn = true;
    setCookies(cookies);
  }

  public void setLoggedOut() {
    this.loggedIn = false;
    clearCookies();
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
    return String.format(
        "UserID: %s, Username: %s, Password: *****, Logged in: %s, Email: %s, Server ID: %s, cookie count: %d",
        getUserId(), getUsername(), isLoggedIn(), getEmail(), getServerId(), cookies.size());
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof final FileServerUser fileServerUser)) {
      return false;
    }
    // Cast for comparison
    final String username = (this.getUsername() != null) ? this.getUsername() : "";
    final String password = (this.getPassword() != null) ? this.getPassword() : "";
    final String email = (this.getEmail() != null) ? this.getEmail() : "";

    return username.equals(fileServerUser.getUsername())
        && password.equals(fileServerUser.getPassword())
        && email.equals(fileServerUser.getEmail());
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, username, password, email, loggedIn, serverId, cookies);
  }
}
