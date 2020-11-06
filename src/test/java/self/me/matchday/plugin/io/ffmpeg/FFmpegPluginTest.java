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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FFmpeg plugin")
@TestMethodOrder(OrderAnnotation.class)
class FFmpegPluginTest {

    private static final String LOG_TAG = "FFmpegPluginTest";

    // Test constants
    private static final String STORAGE_LOCATION = "src/test/data/video_test";
    private static final String TEST_URI = "http://192.168.0.101/stream2stream/barca-rm-2009/1.ts";
    private static final String SAMPLE_METADATA_JSON = "ffprobe_sample_metadata.json";
    // Test resources
    private static File storageLocation;
    private static FFmpegPlugin ffmpegPlugin;
    private static URI testUri;
    private static FFmpegMetadata expectedMetadata;

    @BeforeAll
    static void setUp(@Autowired FFmpegPlugin plugin) throws IOException {

        // Get FFMPEG instance
        ffmpegPlugin = plugin;

        try {
            // Get video storage location
            storageLocation = new File(STORAGE_LOCATION);
            Log.i(LOG_TAG, "Got storage path: " + storageLocation.getAbsolutePath());

            // Create directory if not exists
            if (!storageLocation.exists()) {
                final boolean mkdir = storageLocation.mkdir();
                Log.i(LOG_TAG, String.format("Created directory: [%s] ? : %s", storageLocation, mkdir));
            }

            // Parse test URL
            testUri = new URI(TEST_URI);

        } catch (NullPointerException | URISyntaxException e) {
            e.printStackTrace();
            throw new IOException("Error reading test video storage location!", e);
        }

        // Read test metadata & deserialize
        List<String> sampleMetadata =
                ResourceFileReader.readTextResource(FFprobeTest.class, SAMPLE_METADATA_JSON);
        // Parse JSON to object
        expectedMetadata = new Gson().fromJson(String.join(" ", sampleMetadata), FFmpegMetadata.class);
    }

    @Test
    @Order(1)
    @DisplayName("Test that plugin can stream remote data to correct path in required time")
    void streamUris() throws InterruptedException {

        // Setup test data
        final Path streamingPath = ffmpegPlugin.streamUris(List.of(testUri), storageLocation.toPath());
        Log.i(LOG_TAG,
                String.format("Began streaming to path: [%s] at time: %s",
                        streamingPath.toAbsolutePath(), Instant.now()));

        // Test data
        final int minExpectedFileCount = 10;
        int actualFileCount = 0;

        final Duration timeout = Duration.ofSeconds(30);
        final int fileCheckCycleSecs = 5;
        boolean filesFound = false;
        Duration elapsed = Duration.ZERO;
        Instant start = Instant.now();

        while (!filesFound && elapsed.compareTo(timeout) < 0) {

            // Wait...
            Thread.sleep(fileCheckCycleSecs * 1_000L);
            // Check file count
            actualFileCount = streamingPath.getParent().toFile().list().length;
            Log.i(LOG_TAG, String.format("Found %s files on current sleep cycle...", actualFileCount));
            if (actualFileCount >= minExpectedFileCount) {
                filesFound = true;
            }
            elapsed = Duration.between(start, Instant.now());
        }

        // perform test
        assertThat(actualFileCount).isGreaterThanOrEqualTo(minExpectedFileCount);

    }

    @Test
    @Order(2)
    @DisplayName("Verify that previously started streaming tasks can be successfully interrupted")
    void interruptStreamTasks() {

        Log.i(LOG_TAG, "Attempting to interrupt streaming tasks at time: " + Instant.now());
        ffmpegPlugin.interruptStreamTasks();

    }

    @Test
    @Order(3)
    @DisplayName("Validate reading & parsing of remote video file metadata")
    void readFileMetadata() throws IOException {

        final FFmpegMetadata actualMetadata = ffmpegPlugin.readFileMetadata(testUri);
        Log.i(LOG_TAG, "Validating metadata read from file at URL: " + testUri);

        assertThat(actualMetadata).isEqualTo(expectedMetadata);
    }

    @AfterAll
    @DisplayName("Test teardown")
    static void tearDown() throws IOException, InterruptedException {

        // Ensure all streaming tasks are stopped
        ffmpegPlugin.interruptStreamTasks();

        // Allow time for the process to die
        Thread.sleep(15 * 1_000);

        // Clean up files generated by testing
        Log.i(LOG_TAG, "Cleaning up test data...");
        Files.walkFileTree(storageLocation.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return doDelete(file);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return doDelete(dir);
            }

            private FileVisitResult doDelete(Path path) throws IOException {
                Log.i(LOG_TAG, "Deleting file: " + path.toAbsolutePath());
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}