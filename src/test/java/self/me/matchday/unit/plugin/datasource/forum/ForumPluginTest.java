package self.me.matchday.unit.plugin.datasource.forum;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Match;
import self.me.matchday.model.PlaintextDataSource;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.forum.ForumPlugin;
import self.me.matchday.util.JsonParser;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("ForumPlugin validation tests")
class ForumPluginTest {

    private static final Logger logger = LogManager.getLogger(ForumPluginTest.class);

    private static PlaintextDataSource<Match> testDataSource;

    private final ForumPlugin forumPlugin;

    @Autowired
    ForumPluginTest(ForumPlugin forumPlugin) {
        this.forumPlugin = forumPlugin;
    }

    @BeforeAll
    static void setup() throws IOException {
        final Type type = new TypeReference<PlaintextDataSource<Match>>() {
        }.getType();
        String dsData = ResourceFileReader.readTextResource("data/datasource/test_forum_datasource.json");
        testDataSource = JsonParser.fromJson(dsData, type);
    }

    @Test
    @DisplayName("Verify a Snapshot returns sensible data from a known source")
    void getSnapshot() throws IOException {
        // given
        final int expectedMatchCount = 10;
        SnapshotRequest request = SnapshotRequest.builder().build();

        // when
        logger.info("Getting Snapshot for test...");
        List<Match> matches = forumPlugin.getSnapshot(request, testDataSource).getData().toList();
        final int actualMatchCount = matches.size();
        logger.info("Found {} Matches:", actualMatchCount);

        // then
        matches.forEach(match -> {
            System.out.println(match);
            match.getFileSources().forEach(source -> System.out.println("\t|___> " + source));
        });
        assertThat(actualMatchCount).isNotZero().isEqualTo(expectedMatchCount);
    }

    @Test
    @DisplayName("Ensure plugin properly validates data source")
    void validateDataSource() {
        logger.info("Validating data source: {}", testDataSource);
        assertThat(testDataSource).isNotNull();
        forumPlugin.validateDataSource(testDataSource);
    }

    @Test
    @DisplayName("Validate getting a Snapshot from a specific URL")
    void getUrlSnapshot() throws IOException {
        // given
        final int expectedMatchCount = 1;
        URL testUrl = new URL("http://192.168.0.107:7000/forum/event_01.htm");

        // when
        logger.info("Getting Snapshot from: {}", testUrl);
        Snapshot<Match> snapshot = forumPlugin.getUrlSnapshot(testUrl, testDataSource);

        List<Match> matches = snapshot.getData().toList();
        int actualMatchCount = matches.size();
        logger.info("Found {} Matches: {}", actualMatchCount, matches);

        // then
        assertThat(actualMatchCount).isNotZero().isEqualTo(expectedMatchCount);
        matches.forEach(match -> assertThat(match).isNotNull());
    }

    @Test
    @DisplayName("Validate plugin ID")
    void getPluginId() {
        final UUID expectedPluginId = UUID.fromString("7e230b0d-45a3-4738-8a43-c3b1d6f61df4");
        UUID actualPluginId = forumPlugin.getPluginId();

        logger.info("Validating plugin ID: {}", actualPluginId);
        assertThat(actualPluginId).isNotNull().isEqualTo(expectedPluginId);
    }

    @Test
    @DisplayName("Validate plugin title")
    void getTitle() {
        final String expectedTitle = "Forum";
        final String actualTitle = forumPlugin.getTitle();

        logger.info("Validating plugin title: {}", actualTitle);
        assertThat(actualTitle).isNotNull().isNotEmpty().isEqualTo(expectedTitle);
    }

    @Test
    @DisplayName("Validate plugin description")
    void getDescription() {
        final String expectedDescription = "Scans Events from a forum-based website";
        final String actualDescription = forumPlugin.getDescription();

        logger.info("Validating plugin description: {}", actualDescription);
        assertThat(actualDescription).isNotNull().isNotEmpty().isEqualTo(expectedDescription);
    }
}