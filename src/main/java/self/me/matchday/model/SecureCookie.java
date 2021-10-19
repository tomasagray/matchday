/*
 * Copyright (c) 2021.
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

package self.me.matchday.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.Duration;
import java.util.Objects;

/** Wraps Spring's HttpCookie & ResponseCookie classes for JPA persistence */
@Entity
@Getter
@Setter
@ToString
public class SecureCookie {

  @Id @GeneratedValue private Long id;
  // HttpCookie
  private final String name;

  @Column(columnDefinition = "LONGTEXT")
  private final String value;
  // ResponseCookie
  @Nullable private Duration maxAge;
  @Nullable private String domain;
  @Nullable private String path;
  private boolean secure;
  private boolean httpOnly;
  @Nullable private String sameSite;

  public SecureCookie() {
    this.name = null;
    this.value = null;
  }

  public SecureCookie(@NotNull final String name, @NotNull final String value) {
    this.name = name;
    this.value = value;
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

    if (cookie instanceof ResponseCookie) {
      // Copy response cookie values
      final ResponseCookie responseCookie = (ResponseCookie) cookie;
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
      return ResponseCookie.from(secureCookie.getName(), secureCookie.getValue())
          .maxAge(secureCookie.getMaxAge())
          .domain(secureCookie.getDomain())
          .path(secureCookie.getPath())
          .secure(secureCookie.isSecure())
          .httpOnly(secureCookie.isHttpOnly())
          .sameSite(secureCookie.getSameSite())
          .build();

    } else {
      return new HttpCookie(secureCookie.getName(), secureCookie.getValue());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    SecureCookie that = (SecureCookie) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
