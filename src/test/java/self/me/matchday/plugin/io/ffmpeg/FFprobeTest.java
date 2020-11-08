/*
 * Copyright (c) 2020. 
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.io.ffmpeg;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import self.me.matchday.util.ResourceFileReader;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// TODO - Is this an integration test?
@DisplayName("FFprobeTest - Test the functionality of the FFprobe plugin")
@TestMethodOrder(OrderAnnotation.class)
class FFprobeTest {

    private static final String LOG_TAG = "FFprobeTest";

    // Test constants
    private static final String FFPROBE_PATH = "plugin.ffmpeg.ffprobe-location";
    private static final String TEST_URI = "http://192.168.0.101/stream2stream/barca-rm-2009/1.ts";
    private static final String FFMPEG_PROPERTIES = "plugins\\ffmpeg\\ffmpeg.properties";
    private static final String SAMPLE_METADATA_JSON = "ffprobe_sample_metadata.json";

    // Test resources
    private static Map<String, String> resources;
    private static FFprobe ffProbe;
    private static FFmpegMetadata expectedMetadata;

    @BeforeAll
    static void setUp() throws IOException {

        // Read plugin resources file
        resources = ResourceFileReader.readPropertiesResource(FFprobe.class, FFMPEG_PROPERTIES);
        // Read test metadata & deserialize
        List<String> sampleMetadata =
                ResourceFileReader.readTextResource(FFprobeTest.class, SAMPLE_METADATA_JSON);
        // Parse JSON to object
        expectedMetadata = new Gson().fromJson(String.join(" ", sampleMetadata), FFmpegMetadata.class);
    }

    @Test
    @Order(1)
    @DisplayName("Check creation of FFprobe instance")
    void checkFFprobeCreation() {

        // Create FFprobe instance
        ffProbe = new FFprobe(resources.get(FFPROBE_PATH));

        Log.i(LOG_TAG, "Verifying FFprobe instance is NOT NULL...");
        assertThat(ffProbe).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Verify FFprobe can read remote file metadata")
    void testGetFileMetadata() throws URISyntaxException, IOException {

        Log.i(LOG_TAG, "Reading file data...");
        FFmpegMetadata actualMetadata = ffProbe.getFileMetadata(new URI(TEST_URI));

        Log.i(LOG_TAG, "Testing metadata for correctness...");
        assertThat(actualMetadata).isNotNull();
        assertThat(actualMetadata).isEqualTo(expectedMetadata);
    }
}