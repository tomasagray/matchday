package self.me.matchday.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import self.me.matchday.model.VideoMetadata;
import self.me.matchday.util.Log;

@TestMethodOrder(OrderAnnotation.class)
class VideoMetadataServiceTest {

  private static final String LOG_TAG = "VideoMetadataServiceTest";

  private static final String URL = "http://192.168.0.101/soccer/testing/videos/1st_half.ts";
  private static URL TEST_URL;
  private static VideoMetadataService videoMetadataService;
  private static VideoMetadata videoMetadata;

  @BeforeAll
  static void setup() throws IOException {
    TEST_URL = new URL(URL);
    videoMetadataService = new VideoMetadataService();
  }

  @Test
  @Order(1)
  @DisplayName("Can read metadata from remote source")
  void testParsesDataFromRemoteSource() throws IOException {

    videoMetadata = videoMetadataService.readRemoteData(TEST_URL);
    // Perform test
    assert videoMetadata != null;
    Log.i(LOG_TAG, "VideoMetadata was correctly parsed.");
  }

  @Test
  @Order(2)
  @DisplayName("Can read duration value correctly")
  void testParsesJSONData() {

    // Get the duration value
    final double duration = videoMetadata.getFormat().getDuration();
    // Perform test(s)
    assertEquals(3012.541945d, duration);
    Log.i(LOG_TAG, String.format("Found correct duration: %s; expected: 3012.541945", duration));
  }
}