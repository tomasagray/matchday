package net.tomasbot.matchday.unit.plugin.fileserver.filejoker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import net.tomasbot.matchday.plugin.fileserver.filejoker.FileJokerPlugin;
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
@DisplayName("Validation tests for FileJoker plugin")
public class FileJokerPluginTest {

  private static final Logger logger = LogManager.getLogger(FileJokerPluginTest.class);

  private final FileJokerPlugin plugin;

  @Autowired
  public FileJokerPluginTest(FileJokerPlugin plugin) {
    this.plugin = plugin;
  }

  @Test
  @DisplayName("Test retrieval of plugin hostname")
  void testHostname() throws MalformedURLException {
    final URL expected = new URL("http://testing.matchday.hal9000/filejoker");

    logger.info("Testing hostname...");
    URL hostname = plugin.getHostname();

    assertThat(hostname).isNotNull().isEqualTo(expected);
    logger.info("Found hostname: {}", hostname);
  }

  @Test
  @DisplayName("Test parsing of remaining bandwidth")
  void testGetRemainingBandwidth() throws IOException {
    final float expected = 0.4328f;
    logger.info("Testing parsing of remaining bandwidth...");

    float remainingBandwidth = plugin.getRemainingBandwidth(new HashSet<>());
    assertThat(remainingBandwidth).isEqualTo(expected);
    logger.info("Found remaining bandwidth: {}%", remainingBandwidth * 100f);
  }

  @Test
  @DisplayName("Test parsing of download URL")
  void testGetDownloadUrl() throws IOException {
    final URL testUrl = new URL("http://testing.matchday.hal9000/filejoker/download_landing.php");
    final URL expectedDownloadUrl = new URL("https://expected.url/file.ext");

    logger.info("Testing parsing of download link: {} ...", testUrl);

    Optional<URL> urlOptional = plugin.getDownloadURL(testUrl, new HashSet<>());
    assertThat(urlOptional).isPresent();

    URL downloadUrl = urlOptional.get();
    logger.info("Found download URL: {}", downloadUrl);
    assertThat(downloadUrl).isNotNull().isEqualTo(expectedDownloadUrl);
  }
}
