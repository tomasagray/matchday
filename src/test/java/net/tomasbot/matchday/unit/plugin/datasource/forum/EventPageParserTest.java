package net.tomasbot.matchday.unit.plugin.datasource.forum;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.Match;
import net.tomasbot.matchday.model.PlaintextDataSource;
import net.tomasbot.matchday.model.video.VideoFileSource;
import net.tomasbot.matchday.plugin.datasource.forum.EventPageParser;
import net.tomasbot.matchday.util.JsonParser;
import net.tomasbot.matchday.util.ResourceFileReader;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Forum plugin event page parser validation tests")
class EventPageParserTest {

  private static final Logger logger = LogManager.getLogger(EventPageParserTest.class);

  private static String testData;
  private static PlaintextDataSource<Match> testDataSource;

  private final EventPageParser pageParser;

  @Autowired
  EventPageParserTest(EventPageParser pageParser) {
    this.pageParser = pageParser;
  }

  @BeforeAll
  static void setup() throws IOException {
    testData = ResourceFileReader.readTextResource("data/forum/event_page.html");

    final Type type = new TypeReference<PlaintextDataSource<Match>>() {}.getType();
    String dsData =
        ResourceFileReader.readTextResource("data/datasource/test_forum_datasource.json");
    testDataSource = JsonParser.fromJson(dsData, type);
  }

  @Test
  @DisplayName("Verify parsing Match data from a page")
  void testParseMatchData() {
    // given
    final int expectedFileSourceCount = 2;

    // when
    Event testEvent = pageParser.getEventFrom(testDataSource, testData);
    logger.info("Successfully parsed Event: {}", testEvent);
    Set<VideoFileSource> testFileSources = testEvent.getFileSources();
    int actualFileSourceCount = testFileSources.size();
    logger.info("Found {} VideoFileSources:", actualFileSourceCount);
    testFileSources.forEach(System.out::println);

    // then
    assertThat(testEvent).isNotNull();
    assertThat(actualFileSourceCount).isNotZero().isEqualTo(expectedFileSourceCount);
  }
}
