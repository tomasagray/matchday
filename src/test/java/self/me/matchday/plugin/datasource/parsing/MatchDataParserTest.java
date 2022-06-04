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

package self.me.matchday.plugin.datasource.parsing;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.plugin.datasource.blogger.HtmlBloggerParser;
import self.me.matchday.plugin.datasource.blogger.model.BloggerEntry;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation for MatchDataParser - HTML to Events")
public class MatchDataParserTest {

  private static final String LOG_TAG = "MatchDataParserTest";
  private static final String TEST_DATA_FILE = "data/blogger/blogger_html_single_team.html";

  private static MatchDataParser matchDataParser;

  // test resources
  private static DataSource<Match> testDataSource;
  private static String testHtml;

  @BeforeAll
  static void setup(
      @Autowired @NotNull TestDataCreator testDataCreator,
      @Autowired MatchDataParser matchDataParser) {
    MatchDataParserTest.matchDataParser = matchDataParser;
    MatchDataParserTest.testDataSource =
        testDataCreator.readTestJsonDataSource(); // .readTestHtmlDataSource();
    MatchDataParserTest.readTestResources();
  }

  private static void readTestResources() {
    MatchDataParserTest.testHtml = ResourceFileReader.readTextResource(TEST_DATA_FILE);
    assertThat(testHtml).isNotNull().isNotEmpty();
  }

  private static Stream<Arguments> getBloggerEntryArgs() {
    //    final JsonBloggerParser bloggerParser = new JsonBloggerParser();
    final HtmlBloggerParser bloggerParser = new HtmlBloggerParser();
    return bloggerParser.getBlogger(testHtml).getFeed().getEntry().stream().map(Arguments::of);
  }

  @Test
  @DisplayName("Ensure sample HTML for testing is suitable")
  void validateTestHtml() {

    final int expectedUrlCount = 16;

    final Pattern linkPattern =
        ((PlaintextDataSource<Match>) testDataSource)
            .getPatternKitsFor(URL.class)
            .get(0)
            .getPattern();
    final Document document = Jsoup.parse(testHtml);
    final List<Element> links =
        document.select("a").stream()
            .filter(link -> linkPattern.matcher(link.attr("href")).find())
            .collect(Collectors.toList());

    for (final Element link : links) {
      Log.i(LOG_TAG, "Got link URL: " + link);
      assertThat(link).isNotNull();
    }
    final int actualUrlCount = links.size();
    Log.i(LOG_TAG, "URL count: " + actualUrlCount);
    assertThat(actualUrlCount).isEqualTo(expectedUrlCount);
  }

  @ParameterizedTest(name = "[{index}] Testing with sample data: {0}")
  @MethodSource("getBloggerEntryArgs")
  @DisplayName("Validate each entry can be parsed")
  void testBloggerEntryParsing(@NotNull BloggerEntry entry) {

    final String data = entry.getContent().getData();
    final Stream<? extends Event> events =
        MatchDataParserTest.matchDataParser.getEntityStream(testDataSource, data);

    AtomicInteger elementCount = new AtomicInteger(0);
    events.forEach(
        event -> {
          Log.i(LOG_TAG, "Got Event:\n" + event);
          assertThat(event).isNotNull();
          assertThat(event.getFileSources()).isNotNull();
          assertThat(event.getFileSources().size()).isNotZero();
          assertThat(event.getCompetition()).isNotNull();
          elementCount.getAndIncrement();
        });
    assertThat(elementCount.get()).isNotZero();
  }
}
