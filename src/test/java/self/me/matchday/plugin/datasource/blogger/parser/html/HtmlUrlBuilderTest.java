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

package self.me.matchday.plugin.datasource.blogger.parser.html;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HtmlUrlBuilderTest - Test construction of Blogger HTML URLs")
class HtmlUrlBuilderTest {

    private static final String LOG_TAG = "HtmlUrlBuilderTest";
    // Test constants
    private static final String BASE_URL = "galatamanhdfb.blogspot.com";

    @Test
    @DisplayName("Verify formats label search URL")
    void testLabelSearchUrl() throws MalformedURLException {

        final String searchLabel = "Barcelona";
        final String searchUrl = String.format("https://galatamanhdfb.blogspot.com/search/label/%s", searchLabel);
        final URL actualUrl =
                new HtmlUrlBuilder(BASE_URL)
                        .labels(List.of(searchLabel))
                        .buildUrl();
        final URL expectedUrl = new URL(searchUrl);

        Log.i(LOG_TAG, "Testing URL: " + actualUrl);
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    @DisplayName("Verify formats date search URL")
    void testEndDateSearch() throws MalformedURLException {

        final LocalDateTime testDateTime = LocalDateTime.of(2020, 10, 4, 12, 24);

        final URL actualUrl =
                new HtmlUrlBuilder(BASE_URL)
                        .endDate(testDateTime)
                        .buildUrl();
        final URL expectedUrl =
                new URL("https://galatamanhdfb.blogspot.com/search?updated-max=2020-10-04T12:24:00");

        Log.i(LOG_TAG, "Testing URL: " + actualUrl);
        System.out.println(URLDecoder.decode(expectedUrl.toString(), StandardCharsets.UTF_8));
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    // TODO - Write tests for the other Blogger URL params
}