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

package self.me.matchday.plugin.fileserver.nitroflare;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Nitroflare fileserver manager")
@Disabled
class NitroflarePluginTest {

    private static final String LOG_TAG = "NitroflarePluginTest";
    private static final String TEST_URL =
            "https://nitroflare.com/view/C41524E28CC3151/20200908_denmark-england_0_eng_1080p.ts";
    public static final int REFRESH_HOURS = 24;
    public static final String USER_NAME = "blixblaxblox@protonmail.com";
    public static final String PASSWORD = "3wni(0wxF4qI4KQK";

    private final NitroflarePlugin nitroflarePlugin;

    @Autowired
    NitroflarePluginTest(final NitroflarePlugin nitroflarePlugin) {
        this.nitroflarePlugin = nitroflarePlugin;
    }

    @BeforeAll
    static void setup() {

    }

    @Test
    @DisplayName("Test login function")
    void login() {

        // Create user
        final FileServerUser fileServerUser =
                new FileServerUser(USER_NAME, PASSWORD);
        Log.i(LOG_TAG, "Attempting login with user: " + fileServerUser);

        // Attempt login
        final ClientResponse response = nitroflarePlugin.login(fileServerUser);
        if (response.statusCode().isError()) {
            response.body((inputMessage, context) -> inputMessage.getBody());
        }
        Log.i(LOG_TAG, String.format("Got response: [%s] \n%s\n\nCookies:\n%s", response.statusCode(),
                response.bodyToMono(String.class), response.cookies()));

        // Perform test
        final boolean result = response.statusCode().is2xxSuccessful();
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Test plugin accepts ONLY valid URLs")
    void acceptsUrl() throws IOException {

        // Create valid URL
        final URL validUrl = new URL(TEST_URL);
        // Create invalid URL
        final URL invalidUrl = new URL("http://www.yahoo.com");

        Log.i(LOG_TAG, String.format("Testing URLs: valid (%s), invalid (%s)", validUrl, invalidUrl));

        // Test
        assertThat(nitroflarePlugin.acceptsUrl(validUrl)).isTrue();
        assertThat(nitroflarePlugin.acceptsUrl(invalidUrl)).isFalse();
    }

    @Test
    @DisplayName("Test the data refresh rate is correct")
    void getRefreshRate() {

        final Duration actualRefreshRate = nitroflarePlugin.getRefreshRate();
        final Duration expectedRefreshRate = Duration.ofHours(REFRESH_HOURS);

        Log.i(LOG_TAG, String.format("Testing REFRESH RATE: expected (%s), actual (%s)",
                expectedRefreshRate, actualRefreshRate));

        assertThat(actualRefreshRate).isEqualTo(expectedRefreshRate);
    }

    @Test
    void getDownloadURL() {
        // TODO - Implement Nitroflare download URL validation
    }
}