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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NetscapeCookiesService {

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
  public List<HttpCookie> parseNetscapeCookies(@NotNull final String cookieText) {

    // Split on newline, remove blanks, & map to cookies
    return Arrays.stream(cookieText.split("\n"))
        .filter(this::isCookie)
        .map(this::parseCookie)
        .collect(Collectors.toList());
  }

  /**
   * Parse a Spring HttpCookie from text
   *
   * @param cookieText The text of the cookie (one line)
   * @return A Spring cookie
   */
  private HttpCookie parseCookie(@NotNull final String cookieText) {

    // Split the cookie text on tabs
    final List<String> fields =
        Arrays.stream(cookieText.split("\t"))
            // ... clean
            .map(String::trim)
            .filter(substr -> !("".equals(substr)))
            .collect(Collectors.toList());

    // map to cookie
    return ResponseCookie.from(fields.get(NAME_IDX), fields.get(VALUE_IDX))
        .domain(fields.get(DOMAIN_IDX))
        .path(fields.get(PATH_IDX))
        .secure(toBoolean(fields.get(SECURE_IDX)))
        .maxAge(Long.parseLong(fields.get(EXPIRATION_IDX)))
        .build();
  }

  private boolean isCookie(final String line) {
    return line != null && !("".equals(line)) && !(line.startsWith("#"));
  }

  private boolean toBoolean(@NotNull final String str) {
    return "TRUE".equalsIgnoreCase(str.trim());
  }
}
