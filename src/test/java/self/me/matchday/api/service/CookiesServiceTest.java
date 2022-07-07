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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpCookie;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.util.ResourceFileReader;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Netscape cookie parsing service")
class CookiesServiceTest {

  private static final Logger logger = LogManager.getLogger(CookiesServiceTest.class);
  private static final String COOKIE_FILEPATH = "data/test-netscape-cookies.txt";

  private static CookiesService cookiesService;
  private static String cookieFile;

  @BeforeAll
  static void setUp(@Autowired final CookiesService cookiesService) {

    CookiesServiceTest.cookiesService = cookiesService;

    logger.info("Attempting to read cookie data from: {}", COOKIE_FILEPATH);
    cookieFile = ResourceFileReader.readTextResource(COOKIE_FILEPATH);
    assertThat(cookieFile).isNotNull().isNotEmpty();
    logger.info("Read cookie data from file: {}", COOKIE_FILEPATH);
  }

  @Test
  @DisplayName("Validate Netscape cookie parsing from a String")
  void parseNetscapeCookies() {

    final int expectedCookieCount = 9;

    final Set<HttpCookie> actualCookies = cookiesService.parseCookies(cookieFile);
    assertThat(actualCookies.size()).isEqualTo(expectedCookieCount);

    actualCookies.forEach(
        httpCookie -> {
          logger.info("Testing cookie:\n{}", httpCookie);
          assertThat(httpCookie).isNotNull();
          assertThat(httpCookie.getName()).isNotNull().isNotEmpty();
          assertThat(httpCookie.getValue()).isNotNull().isNotEmpty();
          logger.info("Cookie passed inspection.");
        });
  }
}
