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

package self.me.matchday.unit.plugin.datasource.blogger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static self.me.matchday.plugin.datasource.blogger.BloggerPlugin.SourceType.HTML;
import static self.me.matchday.plugin.datasource.blogger.BloggerPlugin.SourceType.JSON;

import java.time.LocalDate;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.blogger.QueryBuilderService;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate creation of proper Blogger query URIs")
class BloggerQueryBuilderTest {

  private static final Logger logger = LogManager.getLogger(BloggerQueryBuilderTest.class);

  private static QueryBuilderService queryBuilderService;

  @BeforeAll
  static void setUp(@Autowired QueryBuilderService queryBuilder) {
    BloggerQueryBuilderTest.queryBuilderService = queryBuilder;
  }

  @Test
  @DisplayName("Ensure a valid HTML query is built")
  void buildHtmlQueryFrom() {
    final SnapshotRequest testSnapshotRequest = getTestSnapshotRequest();
    logger.info("Building HTML Blogger query with SnapshotRequest:\n{}", testSnapshotRequest);

    final String expectedQuery =
        "/search/label/Something/Something%20Else?"
            + "max-results=25&updated-min=2020-10-11T00:00:00";
    final String actualQuery = queryBuilderService.buildQueryFrom(testSnapshotRequest, HTML);
    logger.info("Got HTML query:\n\t{}", actualQuery);
    assertThat(actualQuery).isEqualTo(expectedQuery);
  }

  @Test
  @DisplayName("Ensure a valid JSON query is built")
  void buildJsonQueryFrom() {
    final SnapshotRequest testSnapshotRequest = getTestSnapshotRequest();
    logger.info("Building JSON Blogger query with SnapshotRequest:\n{}", testSnapshotRequest);

    final String expectedQuery =
        "//-/Something/Something%20Else?"
            + "alt=json&max-results=25&updated-min=2020-10-11T00:00:00";
    final String actualQuery = queryBuilderService.buildQueryFrom(testSnapshotRequest, JSON);
    logger.info("Got JSON query:\n\t{}", actualQuery);
    assertThat(actualQuery).isEqualTo(expectedQuery);
  }

  private SnapshotRequest getTestSnapshotRequest() {
    return SnapshotRequest.builder()
        .labels(List.of("Something", "Something Else"))
        .startDate(LocalDate.of(2020, 10, 11).atStartOfDay())
        .maxResults(25)
        .build();
  }
}
