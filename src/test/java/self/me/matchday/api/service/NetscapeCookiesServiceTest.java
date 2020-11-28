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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpCookie;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Netscape cookie parsing service")
class NetscapeCookiesServiceTest {

  private static final String LOG_TAG = "NetscapeCookiesServiceTest";
  private static final String COOKIE_FILE = "src/test/secure_resources/cookies-nitroflare.txt";

  private static NetscapeCookiesService cookiesService;
  private static String cookieFile;

  @BeforeAll
  static void setUp(@Autowired final NetscapeCookiesService cookiesService) throws IOException {

    NetscapeCookiesServiceTest.cookiesService = cookiesService;

    // Read test resource from disk
    final BufferedReader reader =
        new BufferedReader(new FileReader(COOKIE_FILE));
    cookieFile = reader.lines().collect(Collectors.joining("\n"));

    // Ensure file was read successfully
    assertThat(cookieFile).isNotNull().isNotEmpty();
    Log.i(LOG_TAG, "Read cookie data from file: " + COOKIE_FILE);
  }

  @Test
  @DisplayName("Validate Netscape cookie parsing from a String")
  void parseNetscapeCookies() {

    final int expectedCookieCount = 9;

    final List<HttpCookie> actualCookies = cookiesService.parseNetscapeCookies(cookieFile);
    assertThat(actualCookies.size()).isEqualTo(expectedCookieCount);

    actualCookies.forEach(httpCookie -> {
      Log.i(LOG_TAG, "Testing cookie:\n" + httpCookie + "\n");
      assertThat(httpCookie).isNotNull();
      assertThat(httpCookie.getName()).isNotNull().isNotEmpty();
      assertThat(httpCookie.getValue()).isNotNull().isNotEmpty();
    });
  }
}
