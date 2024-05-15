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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import self.me.matchday.model.SecureCookie;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CookiesService {

  // Cookie field indices
  private static final int DOMAIN_IDX = 0;
  private static final int PATH_IDX = 2;
  private static final int SECURE_IDX = 3;
  private static final int EXPIRATION_IDX = 4;
  private static final int NAME_IDX = 5;
  private static final int VALUE_IDX = 6;

  /**
   * Parse the text of a Netscape cookies.txt file into a collection of Spring cookies
   *
   * @param cookieText The text of cookies.txt
   * @return A List<> of Sprint cookies
   */
  public Set<HttpCookie> parseCookies(@NotNull final String cookieText) {

    // Split on newline, remove blanks, & map to cookies
    return Arrays.stream(cookieText.split("\n"))
        .filter(this::isCookie)
        .map(this::parseCookie)
        .collect(Collectors.toSet());
  }

  /**
   * Parse a Spring HttpCookie from text
   *
   * @param cookieText The text of the cookie (one line)
   * @return A Spring cookie
   */
  public HttpCookie parseCookie(@NotNull final String cookieText) {

    // Split the cookie text on tabs
    final List<String> fields =
        Arrays.stream(cookieText.split("\t"))
            // ... clean
            .map(String::trim)
                .filter(substr -> !(substr.isEmpty()))
            .toList();

    // Extract fields
    final String name = fields.get(NAME_IDX);
    // value may be empty
    final String value = (fields.size() == 7) ? fields.get(VALUE_IDX) : "";
    final String domain = fields.get(DOMAIN_IDX);
    final String path = fields.get(PATH_IDX);
    final boolean secure = toBoolean(fields.get(SECURE_IDX));
    final long maxAge = Long.parseLong(fields.get(EXPIRATION_IDX));

    // map to cookie
    return ResponseCookie.from(name, value)
        .domain(domain)
        .path(path)
        .secure(secure)
        .maxAge(maxAge)
        .build();
  }

  /**
   * Validate a collection of cookies
   *
   * @param cookies The cookies
   */
  public void validateCookies(@NotNull final Collection<SecureCookie> cookies) {
    if (cookies.isEmpty()) {
      throw new InvalidCookieException("Empty cookie collection");
    }
    cookies.forEach(this::validateCookie);
  }

  /**
   * Ensure a cookie meets minimum logical requirements
   *
   * @param cookie The cookie to validate
   */
  public void validateCookie(@NotNull final SecureCookie cookie) {
    final String name = cookie.getName();
    final String path = cookie.getPath();
    if ("".equals(name) || "".equals(path)) {
      throw new InvalidCookieException("Cookie was blank: " + cookie);
    }
  }

  private boolean isCookie(final String line) {
    return line != null && !(line.isEmpty()) && !(line.startsWith("#"));
  }

  private boolean toBoolean(@NotNull final String str) {
    return "TRUE".equalsIgnoreCase(str.trim());
  }
}
