package net.tomasbot.matchday.admin.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;
import net.tomasbot.matchday.api.service.admin.IpInfoDotIo;
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
@DisplayName("Validation tests for ipinfo.io service")
class IpInfoDotIoTest {

  private static final Logger logger = LogManager.getLogger(IpInfoDotIoTest.class);

  private static final Pattern IPV4_PATTERN =
      Pattern.compile(
          "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

  private final IpInfoDotIo ipInfoDotIo;

  @Autowired
  IpInfoDotIoTest(IpInfoDotIo ipInfoDotIo) {
    this.ipInfoDotIo = ipInfoDotIo;
  }

  @Test
  @DisplayName("Test ipinfo.io service retrieval of IPv4 address")
  void testIpInfoDotIo() throws IOException {
    // when
    logger.info("Retrieving IP address from ipinfo.io");
    String ipAddress = ipInfoDotIo.getIpAddress();
    logger.info("Retrieved IP address: {}", ipAddress);

    // then
    assertThat(ipAddress).matches(IPV4_PATTERN);
    logger.info("IP address is valid");
  }

  @Test
  @DisplayName("Test ipinfo.io service retrieval of service name")
  void testName() {
    // given
    final String expectedName = "ipinfo.io";

    // when
    String actualName = ipInfoDotIo.getName();
    logger.info("Service name: {}", actualName);

    // then
    assertThat(actualName).isEqualTo(expectedName);
  }
}
