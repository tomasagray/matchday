package self.me.matchday.admin.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.service.MatchService;
import self.me.matchday.api.service.admin.BackupService;
import self.me.matchday.model.Match;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("BackupService validation tests")
class BackupServiceTest {

    private static final Logger logger = LogManager.getLogger(BackupServiceTest.class);

    private static final List<Path> cleanupFiles = new ArrayList<>();
    private final BackupService backupService;
    private final TestDataCreator testDataCreator;
    private final MatchService matchService;

    @Autowired
    BackupServiceTest(
            BackupService backupService,
            TestDataCreator testDataCreator,
            MatchService matchService) {
        this.backupService = backupService;
        this.testDataCreator = testDataCreator;
        this.matchService = matchService;
    }

    @AfterAll
    static void cleanup() throws IOException {
        logger.info("Cleaning up test data...");
        for (final Path path : cleanupFiles) {
            File file = path.toFile();
            boolean deleted = file.delete();
            if (!deleted || file.exists()) {
                throw new IOException("Could not delete file: " + path);
            }
        }
    }

    @Test
    @DisplayName("Validate creation of backup archive")
    void testCreateBackup() throws IOException {

        // given
        final long expectedFilesize = 560_000L;

        // when
        logger.info("Creating application backup...");
        Path backup = backupService.createBackup();
        logger.info("Created backup at: " + backup);
        cleanupFiles.add(backup);

        // then
        assertThat(backup.toFile()).exists();
        long actualFilesize = backup.toFile().length();
        logger.info("Filesize: {}", actualFilesize);
        assertThat(actualFilesize).isGreaterThanOrEqualTo(expectedFilesize);
    }

    @Test
    @DisplayName("Validate restoration of backup functionality")
    void testRestore() throws IOException, SQLException {

        // given
        int additionalMatchCount = 3;
        int beforeMatchCount = matchService.fetchAll().size();
        assertThat(beforeMatchCount).isNotZero();

        // when
        Path backup = backupService.createBackup();
        assertThat(backup.toFile()).exists();
        logger.info("Created test backup at: {}", backup);
        cleanupFiles.add(backup);
        // create additional matches
        List<Match> additionalMatches = IntStream.range(0, additionalMatchCount)
                .mapToObj(i -> testDataCreator.createTestMatch("Additional Match " + i))
                .collect(Collectors.toList());
        matchService.saveAll(additionalMatches);
        int afterMatchCount = matchService.fetchAll().size();
        assertThat(afterMatchCount - beforeMatchCount).isEqualTo(additionalMatches.size());

        // then
        logger.info("Restoring backup from: {}", backup);
        backupService.loadBackupArchive(backup);
        logger.info("Backup restore complete");
        int restoredMatchCount = matchService.fetchAll().size();
        logger.info("After restore, there are: {} Matches", restoredMatchCount);
        assertThat(restoredMatchCount).isEqualTo(beforeMatchCount);
    }

    @Test
    @DisplayName("Validate dehydrating system")
    void testDehydrate() throws IOException {

        // given
        int expectedFilesize = 100_000;

        // when
        logger.info("Dehydrating system...");
        Path dehydrated = backupService.dehydrateToDisk();
        assertThat(dehydrated).isNotNull();
        assertThat(dehydrated.toFile()).exists();
        cleanupFiles.add(dehydrated);

        // then
        logger.info("Dehydrated system to: " + dehydrated);
        String json = Files.readString(dehydrated);
        int dataSize = json.getBytes(StandardCharsets.UTF_8).length;
        logger.info("Dehydrated data is {} bytes", dataSize);
        assertThat(json).isNotNull().isNotEmpty();
        assertThat(dataSize).isGreaterThanOrEqualTo(expectedFilesize);
    }
}