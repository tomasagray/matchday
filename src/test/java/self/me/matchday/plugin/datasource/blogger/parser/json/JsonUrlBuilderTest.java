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

package self.me.matchday.plugin.datasource.blogger.parser.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonUrlBuilderTest {

  public static final String SEARCH_DATETIME = "2020-10-05T14:46:00";
  private static final String LOG_TAG = "JsonUrlBuilderTest";
  // Test constants
  private static final String SEARCH_TERM = "Barcelona";
  private static final String BASE_URL = "zkfullmatchvideos.blogspot.com";

  @Test
  @DisplayName("Validate label search URL formatting")
  void testLabelSearch() throws MalformedURLException {

    final URL actualUrl = new JsonUrlBuilder(BASE_URL).labels(List.of(SEARCH_TERM)).buildUrl();
    final String expectedLink =
        String.format(
            "https://zkfullmatchvideos.blogspot.com/feeds/posts/default?alt=json&q=%s",
            SEARCH_TERM);
    final URL expectedUrl = new URL(expectedLink);

    Log.i(LOG_TAG, "Testing URL: " + actualUrl);
    assertThat(actualUrl).isEqualTo(expectedUrl);
  }

  @Test
  @DisplayName("Validate date search URL formatting")
  void testDateSearch() throws MalformedURLException {

    final LocalDateTime testDateTime = LocalDateTime.parse(SEARCH_DATETIME);
    final URL actualUrl = new JsonUrlBuilder(BASE_URL).endDate(testDateTime).buildUrl();
    final URL expectedUrl =
        new URL(
            "https://zkfullmatchvideos.blogspot.com/feeds/posts/default?alt=json&updated-max="
                + SEARCH_DATETIME);

    Log.i(LOG_TAG, "Testing URL: " + actualUrl);
    assertThat(actualUrl).isEqualTo(expectedUrl);
  }

  // TODO: Write tests for other types of searches
}
