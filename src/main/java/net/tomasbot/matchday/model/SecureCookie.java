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

import java.time.Duration;
import java.util.Objects;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;

/** Wraps Spring's HttpCookie & ResponseCookie classes for JPA persistence */
@Entity
@Getter
@Setter
@ToString
public class SecureCookie {

  // HttpCookie
  private final String name;

  @Column(columnDefinition = "LONGTEXT")
  private final String cookieValue;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // ResponseCookie
  @Nullable private Duration maxAge;
  @Nullable private String domain;
  @Nullable private String path;
  private boolean secure;
  private boolean httpOnly;
  @Nullable private String sameSite;

  public SecureCookie() {
    this.name = null;
    this.cookieValue = null;
  }

  public SecureCookie(@NotNull final String name, @NotNull final String cookieValue) {
    this.name = name;
    this.cookieValue = cookieValue;
  }

  /**
   * Map a Spring HttpCookie to a SecureCookie for encryption & persistence
   *
   * @param cookie The Spring cookie
   * @return A SecureCookie
   */
  public static @NotNull SecureCookie fromSpringCookie(@NotNull final HttpCookie cookie) {
    // Create base instance
    final SecureCookie secureCookie = new SecureCookie(cookie.getName(), cookie.getValue());

    if (cookie instanceof final ResponseCookie responseCookie) {
      // Copy response cookie values
      secureCookie.setMaxAge(responseCookie.getMaxAge());
      secureCookie.setDomain(responseCookie.getDomain());
      secureCookie.setPath(responseCookie.getPath());
      secureCookie.setSecure(responseCookie.isSecure());
      secureCookie.setHttpOnly(responseCookie.isHttpOnly());
      secureCookie.setSameSite(responseCookie.getSameSite());
    }

    return secureCookie;
  }

  /**
   * Map a SecureCookie back to a Spring HttpCookie
   *
   * @param secureCookie The SecureCookie to be converted to a Spring cookie
   * @return A Spring HttpCookie
   */
  public static @NotNull HttpCookie toSpringCookie(@NotNull final SecureCookie secureCookie) {
    // If secure cookie is a response cookie
    if ((secureCookie.getMaxAge() != null)
        && (secureCookie.getDomain() != null)
        && (secureCookie.getPath() != null)) {

      // Create response cookie
      return ResponseCookie.from(secureCookie.getName(), secureCookie.getCookieValue())
          .maxAge(secureCookie.getMaxAge())
          .domain(secureCookie.getDomain())
          .path(secureCookie.getPath())
          .secure(secureCookie.isSecure())
          .httpOnly(secureCookie.isHttpOnly())
          .sameSite(secureCookie.getSameSite())
          .build();

    } else {
      return new HttpCookie(secureCookie.getName(), secureCookie.getCookieValue());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SecureCookie that)) return false;
    if (this == o) return true;
    if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(),
        getName(),
        getCookieValue(),
        getMaxAge(),
        getDomain(),
        getPath(),
        isSecure(),
        isHttpOnly(),
        getSameSite());
  }
}
