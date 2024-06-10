package self.me.matchday.unit.plugin.datasource.forum;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.plugin.datasource.forum.EventListParser;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.ResourceFileReader;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Forum scanning plugin Event list parser validation tests")
class EventListParserTest {

  private static final Logger logger = LogManager.getLogger(EventListParserTest.class);

  private static PlaintextDataSource<Match> testDataSource;
  private static String testData;

  private final EventListParser eventListParser;

  @Autowired
  EventListParserTest(EventListParser eventListParser) {
    this.eventListParser = eventListParser;
  }

  @BeforeAll
  static void setup() throws IOException {
    testData = ResourceFileReader.readTextResource("data/forum/event_list.html");
    final Type type = new TypeReference<PlaintextDataSource<Match>>() {}.getType();
    String dsData =
        ResourceFileReader.readTextResource("data/datasource/test_forum_datasource.json");
    testDataSource = JsonParser.fromJson(dsData, type);
  }

  @Test
  @DisplayName("Verify a list of Matches can be read from sample HTML")
  void getEventsList() {
    // given
    final int expectedEventCount = 10;

    // when
    logger.info("Testing parsing of Events from sample HTML...");
    Map<URI, ? extends Event> events = eventListParser.getEventsList(testData, testDataSource);
    int actualEventCount = events.size();
    logger.info("Found: {} Matches...", actualEventCount);
    events.forEach((href, event) -> System.out.println(event));

    // then
    assertThat(actualEventCount).isNotZero().isEqualTo(expectedEventCount);
  }
}
