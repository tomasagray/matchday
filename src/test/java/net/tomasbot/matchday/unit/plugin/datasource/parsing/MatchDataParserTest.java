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

package net.tomasbot.matchday.unit.plugin.datasource.parsing;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.Match;
import net.tomasbot.matchday.model.PlaintextDataSource;
import net.tomasbot.matchday.plugin.datasource.blogger.HtmlBloggerParser;
import net.tomasbot.matchday.plugin.datasource.blogger.model.BloggerEntry;
import net.tomasbot.matchday.plugin.datasource.parsing.MatchDataParser;
import net.tomasbot.matchday.util.ResourceFileReader;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation for MatchDataParser - HTML to Events")
public class MatchDataParserTest {

  private static final Logger logger = LogManager.getLogger(MatchDataParserTest.class);
  private static final String TEST_DATA_FILE = "data/blogger/blogger_html_single_team.html";
  private static String testHtml;
  private final MatchDataParser matchDataParser;
  // test resources
  private final DataSource<Match> testDataSource;

  @Autowired
  public MatchDataParserTest(
      @NotNull TestDataCreator testDataCreator, MatchDataParser matchDataParser)
      throws IOException {
    this.matchDataParser = matchDataParser;
    this.testDataSource = testDataCreator.readTestLiveDataSource(); // .readTestHtmlDataSource();
    testHtml = readTestResources();
  }

  private static @NotNull String readTestResources() throws IOException {
    final String html = ResourceFileReader.readTextResource(TEST_DATA_FILE);
    assertThat(html).isNotNull().isNotEmpty();
    return html;
  }

  private static Stream<Arguments> getBloggerEntryArgs() throws IOException {
    if (testHtml == null) {
      testHtml = readTestResources();
    }
    final HtmlBloggerParser bloggerParser = new HtmlBloggerParser();
    final List<BloggerEntry> entries = bloggerParser.parseBlogger(testHtml).getFeed().getEntry();
    logger.info("Found: {} Blogger entries for testing...", entries.size());
    return entries.stream().map(Arguments::of);
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
            .toList();

    for (final Element link : links) {
      logger.info("Got link URL: {}", link);
      assertThat(link).isNotNull();
    }
    final int actualUrlCount = links.size();
    logger.info("URL count: {}", actualUrlCount);
    assertThat(actualUrlCount).isEqualTo(expectedUrlCount);
  }

  @ParameterizedTest(name = "[{index}] Testing with sample data: {0}")
  @MethodSource("getBloggerEntryArgs")
  @DisplayName("Validate each entry can be parsed")
  void testBloggerEntryParsing(@NotNull BloggerEntry entry) {
    final String data = entry.getContent().getData();
    final List<? extends Event> events =
        matchDataParser.getEntityStream(testDataSource, data).toList();
    final int actualEventCount = events.size();
    logger.info("Found: {} Events in current data", actualEventCount);
    assertThat(actualEventCount).isNotZero();

    events.forEach(
        event -> {
          logger.info("Got Event:\n{}", event);
          assertThat(event).isNotNull();
          assertThat(event.getFileSources()).isNotNull();
          assertThat(event.getFileSources().size()).isNotZero();
          assertThat(event.getCompetition()).isNotNull();
        });
  }
}
