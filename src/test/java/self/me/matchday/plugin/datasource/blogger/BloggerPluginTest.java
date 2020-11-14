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

package self.me.matchday.plugin.datasource.blogger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.blogger.parser.html.HtmlBuilderFactory;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for Blogger plugin")
class BloggerPluginTest {

    private static final String LOG_TAG = "BloggerPluginTest";

    private static BloggerPlugin bloggerPlugin;

    @BeforeAll
    static void setUp(@Autowired BloggerPlugin plugin) {
        // Inject plugin
        bloggerPlugin = plugin;
    }

    @Test
    @DisplayName("Validate plugin ID")
    void testPluginId() {

        final UUID actualPluginId = bloggerPlugin.getPluginId();
        final UUID expectedPluginId = UUID.fromString("64d08bc8-bd9f-11ea-b3de-0242ac130004");

        Log.i(LOG_TAG, "Testing plugin ID: " + actualPluginId);
        assertThat(actualPluginId).isEqualTo(expectedPluginId);
    }

    @Test
    @DisplayName("Validate plugin title")
    void testTitle() {

        final String actualTitle = bloggerPlugin.getTitle();
        final String expectedTitle = "Blogger";

        Log.i(LOG_TAG, "Testing Blogger plugin title: " + actualTitle);
        assertThat(actualTitle).isEqualTo(expectedTitle);
    }

    @Test
    @DisplayName("Validate plugin description")
    void testDescription() {

        final String actualDescription = bloggerPlugin.getDescription();
        final String expectedDescription = "Reads a Blogger blog from either HTML or JSON sources, and makes it " +
                "available to the containing application as a POJO. Implements the DataSourcePlugin<> interface.";

        Log.i(LOG_TAG, "Testing Blogger plugin description:\n" + actualDescription);
        assertThat(actualDescription).isEqualTo(expectedDescription);
    }

    @Test
    @DisplayName("Validate Snapshot request handling")
    void testSnapshot() throws IOException {

        final int expectedPostCount = 5;

        // Create SnapshotRequest
        final SnapshotRequest snapshotRequest =
                SnapshotRequest
                        .builder()
                        .labels(List.of("Barcelona"))
                        .build();
        // Setup BloggerParserFactory
        bloggerPlugin.setBloggerBuilderFactory(new HtmlBuilderFactory());
        // Set base URL
        bloggerPlugin.setBaseUrl("galatamanhdfb.blogspot.com");
        final Snapshot<Blogger> snapshot = bloggerPlugin.getSnapshot(snapshotRequest);
        // Extract result
        final Instant actualTimestamp = snapshot.getTimestamp();
        final Instant expectedTimestamp = Instant.now();
        final Blogger actualData = snapshot.getData();
        final String expectedTitle = "GaLaTaMaN HD Football: Barcelona";

        Log.i(LOG_TAG, "Testing snapshot: " + actualData);
        Log.i(LOG_TAG, "Testing snapshot timestamp: " + actualTimestamp);
        assertThat(actualTimestamp).isCloseTo(expectedTimestamp, within(5, ChronoUnit.SECONDS));
        assertThat(actualData.getPostCount()).isEqualTo(expectedPostCount);
        assertThat(actualData.getTitle()).isEqualTo(expectedTitle);
    }
}