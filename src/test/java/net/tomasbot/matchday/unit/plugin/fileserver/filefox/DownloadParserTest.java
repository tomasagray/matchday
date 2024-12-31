package net.tomasbot.matchday.unit.plugin.fileserver.filefox;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import net.tomasbot.matchday.plugin.fileserver.filefox.DownloadParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Validate Filefox download parser correctly processes download page")
@Disabled
class DownloadParserTest {

  // TODO: implement this test with SimpleWebServer, Java 18

  private static final Logger logger = LogManager.getLogger(DownloadParserTest.class);

  private final DownloadParser downloadParser;

  @Autowired
  DownloadParserTest(DownloadParser downloadParser) {
    this.downloadParser = downloadParser;
  }

  @Test
  void parseDownloadRequest() throws Exception {

    URL landing =
        getClass().getClassLoader().getResource("data/filefox/FileFox_DownloadLanding.htm");
    assertThat(landing).isNotNull();

    URL downloadUrl =
        downloadParser.parseDownloadRequest(landing.toURI(), new LinkedMultiValueMap<>());
    logger.info("Found download URL: {}", downloadUrl);
  }
}
