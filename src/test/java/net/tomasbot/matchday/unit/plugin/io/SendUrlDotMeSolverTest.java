package net.tomasbot.matchday.unit.plugin.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;
import net.tomasbot.matchday.plugin.io.url.SendUrlDotMeSolver;
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
@DisplayName("Validation tests for SendUrlDotMeSolver plugin")
class SendUrlDotMeSolverTest {

  private static final Logger logger = LogManager.getLogger(SendUrlDotMeSolverTest.class);

  private static final Pattern URL_PATTERN =
      Pattern.compile(
          "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");

  private final SendUrlDotMeSolver sendUrlDotMeSolver;

  @Autowired
  SendUrlDotMeSolverTest(SendUrlDotMeSolver sendUrlDotMeSolver) {
    this.sendUrlDotMeSolver = sendUrlDotMeSolver;
  }

  @Test
  @DisplayName("Verify SendUrlDotMeSolver returns a valid URL")
  void testSendUrlDotMeSolver() throws IOException {
    // given
    URL testUrl = new URL("https://sendurl.me/zkEERyq7");

    // when
    URL actualSolvedUrl = sendUrlDotMeSolver.resolveUrl(testUrl);
    logger.info("Actual solved URL: {}", actualSolvedUrl);

    // then
    assertThat(actualSolvedUrl).isNotEqualTo(testUrl);
    assertThat(actualSolvedUrl.toString()).matches(URL_PATTERN);
    logger.info("Solved URL is valid.");
  }
}
