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

package self.me.matchday.plugin.datasource.galataman;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.util.Log;

import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate Galataman pattern loading")
class GManPatternsTest {

    private static final String LOG_TAG = "GManPatternsTest";
    
    private static GManPatterns gManPatterns;
    
    @BeforeAll
    static void setUp(@Autowired final GManPatterns _gManPatterns) {
        // save autowired reference
        gManPatterns = _gManPatterns;
    }

    @Test
    @DisplayName("Validate A/V delimiter is the correct character sequence")
    void testAvDataDelimiter() {

        final String actualAVDelimiter = gManPatterns.getAvDataDelimiter();
        final String expectedAVDelimiter = "‖";

        Log.i(LOG_TAG, "Testing A/V delimiter: " + actualAVDelimiter);

        assertThat(actualAVDelimiter)
                .isNotNull()
                .isNotEmpty();
        assertThat(actualAVDelimiter).isEqualTo(expectedAVDelimiter);
    }

    @Test
    @DisplayName("Validate channel pattern correctly parses audio channels")
    void testChannelMatcher() {

        final String TEST_CHANNEL = "5.1 channels";

        final String actualChannelPattern = gManPatterns.getChannel();
        Log.i(LOG_TAG, "Testing channel pattern: " + actualChannelPattern);

        assertThat(actualChannelPattern)
                .isNotNull()
                .isNotEmpty();

        // test pattern
        final Matcher surroundChannelMatcher = gManPatterns.getChannelMatcher(TEST_CHANNEL);
        final boolean channelFound = surroundChannelMatcher.find();
        assertThat(channelFound).isTrue();
        assertThat(surroundChannelMatcher.group(1)).isEqualTo("5.1");
    }

    @Test
    @DisplayName("Validate bitrate pattern correctly parses video bitrate")
    void testBitrateMatcher() {

        final String TEST_MBPS = "4 Mbps";
        final String TEST_KBPS = "448 Kbps";

        final String actualBitratePattern = gManPatterns.getBitrate();
        Log.i(LOG_TAG, "Testing  bitrate pattern: " + actualBitratePattern);

        assertThat(actualBitratePattern)
                .isNotNull()
                .isNotEmpty();

        // test pattern
        final boolean foundMbps = gManPatterns.getBitrateMatcher(TEST_MBPS).find();
        final boolean foundKbps = gManPatterns.getBitrateMatcher(TEST_KBPS).find();
        assertThat(foundMbps).isTrue();
        assertThat(foundKbps).isTrue();
    }

    @Test
    @DisplayName("Validate video container pattern parses video container")
    void testContainerMatcher() {

        final String TEST_CONTAINER = "H.264 mkv";

        final String actualContainerPattern = gManPatterns.getContainer();
        Log.i(LOG_TAG, "Testing container pattern: " + actualContainerPattern);

        assertThat(actualContainerPattern)
                .isNotNull()
                .isNotEmpty();

        // test pattern
        final boolean foundContainer = gManPatterns.getContainerMatcher(TEST_CONTAINER).find();
        assertThat(foundContainer).isTrue();
    }

    @Test
    @DisplayName("Ensure video framerate pattern correctly parses framerate")
    void testFramerateMatcher() {

        final String TEST_FPS = "25fps";

        final String actualFrameratePattern = gManPatterns.getFramerate();
        Log.i(LOG_TAG, "Testing framerate pattern: " + actualFrameratePattern);

        assertThat(actualFrameratePattern)
                .isNotNull()
                .isNotEmpty();

        // test pattern
        final boolean foundFps = gManPatterns.getFramerateMatcher(TEST_FPS).find();
        assertThat(foundFps).isTrue();
    }

    @Test
    @DisplayName("Validate bitrate conversion factor correctly converts bitrate units")
    void testBitrateConversionFactor() {

        final Long actualBitrateConversionFactor = gManPatterns.getBitrateConversionFactor();
        final Long expectedBitrateConversionFactor = 1_000_000L;

        Log.i(LOG_TAG, "Testing bitrate conversion factor: " + actualBitrateConversionFactor);
        assertThat(actualBitrateConversionFactor).isEqualTo(expectedBitrateConversionFactor);
    }
}