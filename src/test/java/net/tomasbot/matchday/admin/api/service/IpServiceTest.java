package net.tomasbot.matchday.admin.api.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import net.tomasbot.matchday.api.service.admin.IpService;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class IpServiceTest {

  private static final Logger logger = LogManager.getLogger(IpServiceTest.class);
  private final IpService ipService;
  private IpServiceTestData data;

  @Autowired
  IpServiceTest(IpService ipService) {
    this.ipService = ipService;
  }

  private @NotNull IpServiceTest.IpServiceTestData fetchTestData() {
    try {
      final Instant start = Instant.now();
      final String ipAddress = ipService.getIpAddress();
      final Instant end = Instant.now();
      return new IpServiceTestData(ipAddress, Duration.between(start, end));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private IpServiceTestData getTestData() {
    if (this.data == null) {
      this.data = fetchTestData();
    }
    return this.data;
  }

  @Test
  @DisplayName("Validate that a rational IP address can be obtained by the service")
  void testGetIpAddress() {
    // given
    final Pattern ipPattern = Pattern.compile("[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}");
    final IpServiceTestData testData = getTestData();
    final String ipAddress = testData.ip();

    // when
    final Matcher ipMatcher = ipPattern.matcher(ipAddress);
    logger.info("Retrieved IP address: {}", ipAddress);

    // then
    assertThat(ipMatcher.find()).isTrue();
  }

  @Test
  @DisplayName("Verify retrieving IP address completes within a reasonable time (5s)")
  void testIpRetrievalTime() {
    // given
    final Duration maxTestTime = Duration.ofSeconds(5);
    final IpServiceTestData testData = getTestData();
    final Duration testDuration = testData.duration();

    // then
    logger.info("Read IP address: {}", testData.ip());
    logger.info("Retrieving IP address took: {}ms", testDuration.toMillis());
    assertThat(testDuration).isLessThan(maxTestTime);
  }

  private record IpServiceTestData(String ip, Duration duration) {}
}
