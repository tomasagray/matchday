package self.me.matchday.unit.plugin.datasource.forum;

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
import self.me.matchday.plugin.datasource.forum.EventListParser;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Forum scanning plugin Event list parser validation tests")
class EventListParserTest {

    private static String testData;

    private static final Logger logger = LogManager.getLogger(EventListParserTest.class);

    private final EventListParser eventListParser;

    @Autowired
    EventListParserTest(EventListParser eventListParser) {
        this.eventListParser = eventListParser;
    }

    @BeforeAll
    static void setup() throws IOException {
        String filename = "data/forum/event_list.html";
        testData = ResourceFileReader.readTextResource(filename);
        logger.info("Successfully read test data from: {}", filename);
    }

    @Test
    @DisplayName("Verify a list of Matches can be read from sample HTML")
    void getEventsList() {
        // given
        final int expectedEventCount = 10;

        // when
        logger.info("Testing parsing of Events from sample HTML...");
        Map<URL, ? extends Event> events = eventListParser.getEventsList(testData);
        int actualEventCount = events.size();
        logger.info("Found: {} Matches...", actualEventCount);
        events.forEach((href, event) -> System.out.println(event));

        // then
        assertThat(actualEventCount).isNotZero().isEqualTo(expectedEventCount);
    }
}