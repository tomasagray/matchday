package net.tomasbot.matchday.unit.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import net.tomasbot.matchday.common.flaresolverr.FlaresolverrService;
import net.tomasbot.matchday.common.flaresolverr.FlaresolverrSolution;
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
@DisplayName("Verification tests for the Flaresolverr Service")
class FlaresolverrServiceTest {

  private static final Logger logger = LogManager.getLogger(FlaresolverrServiceTest.class);

  private final FlaresolverrService flaresolverrService;

  @Autowired
  FlaresolverrServiceTest(FlaresolverrService flaresolverrService) {
    this.flaresolverrService = flaresolverrService;
  }

  @Test
  @DisplayName("Verify Flaresolverr can solve challenge & return a valid page")
  void solveUrlWithFlaresolverr() throws Exception {
    // given
    final URL testUrl = new URL("https://www.google.com");
    final int expectedBytes = 25 * 1_024;
    final int expectedReturnCode = 200;

    // when
    logger.info("Testing Flaresolverr with URL: {}", testUrl);
    FlaresolverrSolution response = flaresolverrService.solveChallengeFor(testUrl);
    logger.debug("Flaresolverr Response:\n{}", response);
    int responseStatus = response.getStatus();
    logger.info("Flaresolverr responded with status: {}", responseStatus);

    String data = response.getData();
    int byteCount = data.getBytes(StandardCharsets.UTF_8).length;
    logger.info("Read {} bytes", byteCount);

    // then
    assertThat(responseStatus).isEqualTo(expectedReturnCode);
    assertThat(data).isNotBlank();
    assertThat(byteCount).isGreaterThanOrEqualTo(expectedBytes);
  }
}
