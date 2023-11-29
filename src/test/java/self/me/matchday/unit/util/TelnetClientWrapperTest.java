package self.me.matchday.unit.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.util.TelnetClientWrapper;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class TelnetClientWrapperTest {

  private static final Logger logger = LogManager.getLogger(TelnetClientWrapperTest.class);
  private static final String TERMINATOR = "\n.";
  private static final String TEST_HOST = "telehack.com";
  private static final int TEST_PORT = 23;
  private static final int WAIT_SECONDS = 2;

  private final TelnetClientWrapper client;

  @Autowired
  TelnetClientWrapperTest(TelnetClientWrapper client) {
    this.client = client;
  }

  @Test
  @DisplayName("Validate client sending simple command to service")
  void testSimpleConnection() throws IOException {

    // when
    logger.info("Testing telnet connection to: {}:{}...", TEST_HOST, TEST_PORT);
    client.connect(TEST_HOST, TEST_PORT);
    final String connected = client.receive(TERMINATOR);
    logger.info("Connected! Got response:\n{}", connected);

    // then
    assertThat(connected).isNotNull().isNotEmpty();

    // cleanup
    client.disconnect();
  }

  @Test
  @DisplayName("Ensure repeating commands behaves as expected")
  void testRepeatedCommand() throws Exception {

    // given
    logger.info("Connecting to: {}:{}...", TEST_HOST, TEST_PORT);
    client.connect(TEST_HOST, TEST_PORT);
    logger.info("Successfully connected!");
    String loginMsg = client.receive(TERMINATOR);
    logger.info("Got login message:\n{}", loginMsg);

    // when
    client.send("when");
    final String response1 = client.receive(TERMINATOR);
    logger.info("Command: 'when' got response: {}", response1);

    logger.info("Waiting {} seconds before repeating command...", WAIT_SECONDS);
    TimeUnit.SECONDS.sleep(WAIT_SECONDS);
    client.send("when");
    final String response2 = client.receive(TERMINATOR);
    logger.info("Repeated command: 'when' got response: {}", response2);

    // then
    assertThat(response1).isNotNull().isNotEmpty();
    assertThat(response2).isNotNull().isNotEmpty();
    assertThat(response1).isNotEqualTo(response2);

    // cleanup
    client.disconnect();
  }
}
