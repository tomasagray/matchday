package net.tomasbot.matchday.admin.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;
import net.tomasbot.matchday.api.service.admin.CanYouSeeMe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Validation tests for canyouseeme.org service")
class CanYouSeeMeTest {

  private static final Logger logger = LogManager.getLogger(CanYouSeeMeTest.class);

  private static final Pattern IPV4_PATTERN =
      Pattern.compile(
          "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

  private final CanYouSeeMe canYouSeeMe;

  @Autowired
  CanYouSeeMeTest(CanYouSeeMe canYouSeeMe) {
    this.canYouSeeMe = canYouSeeMe;
  }

  @Test
  @DisplayName("Verify that getIpAddress returns a valid IP address")
  void getIpAddress() throws IOException {
    // when
    logger.info("Retrieving IP address from canyouseeme.org...");
    String ipAddress = canYouSeeMe.getIpAddress();
    logger.info("IP address retrieved: {}", ipAddress);

    assertThat(ipAddress).matches(IPV4_PATTERN);
    logger.info("IP address is valid.");
  }

  @Test
  @DisplayName("Validate service name")
  void getName() {
    // given
    final String expectedName = "CanYouSeeMe.org";

    // when
    String actualName = canYouSeeMe.getName();
    logger.info("Service name: {}", actualName);

    // then
    assertThat(actualName).isEqualTo(expectedName);
  }
}
