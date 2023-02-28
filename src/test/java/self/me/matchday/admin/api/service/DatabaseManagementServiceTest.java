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
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.api.service.MatchService;
import self.me.matchday.api.service.PatternKitTemplateService;
import self.me.matchday.api.service.admin.DatabaseManagementService;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.Match;
import self.me.matchday.model.PatternKitTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("DatabaseManagementService validation tests")
class DatabaseManagementServiceTest {

    private static final Logger logger = LogManager.getLogger(DatabaseManagementServiceTest.class);

    private static final List<Path> cleanupPaths = new ArrayList<>();
    private final DatabaseManagementService databaseMgmtSvc;
    private final MatchService eventService;
    private final PatternKitTemplateService templateService;
    private final FileServerUserService userService;
    private final DataSourceService dataSourceService;

    @Autowired
    DatabaseManagementServiceTest(
            DatabaseManagementService databaseMgmtSvc,
            MatchService eventService,
            PatternKitTemplateService templateService,
            FileServerUserService userService,
            DataSourceService dataSourceService) {
        this.databaseMgmtSvc = databaseMgmtSvc;
        this.eventService = eventService;
        this.templateService = templateService;
        this.userService = userService;
        this.dataSourceService = dataSourceService;
    }

    @AfterAll
    static void cleanup() throws IOException {
        logger.info("Cleaning up test data...");
        for (final Path cleanupPath : cleanupPaths) {
            final File cleanupFile = cleanupPath.toFile();
            logger.info("Deleting test file: {}", cleanupFile);

            boolean deleted = cleanupFile.delete();
            if (!deleted || cleanupFile.exists()) {
                throw new IOException("Could not delete test file: " + cleanupFile);
            }
        }
    }

    @Test
    @DisplayName("Validate creating a MySQL dump file (.sql)")
    void createDatabaseDump() throws IOException {

        // when
        logger.info("Creating test database dump...");
        Path dumpPath = databaseMgmtSvc.createDatabaseDump();
        logger.info("Database successfully dumped to: {}", dumpPath);

        // then
        assertThat(dumpPath).isNotNull();
        final File dumpFile = dumpPath.toFile();
        assertThat(dumpFile).exists();
        cleanupPaths.add(dumpPath);
        assertThat(dumpFile.length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Check re-install database functionality")
    void testDropDatabase() throws IOException, SQLException {

        // given
        List<Match> beforeEvents = eventService.fetchAll();
        List<PatternKitTemplate> beforeTemplates = templateService.fetchAll();
        List<FileServerUser> beforeUsers = userService.getAllUsers();
        List<DataSource<?>> beforeDataSources = dataSourceService.fetchAll();

        int beforeMatchCount = beforeEvents.size();
        int beforeTemplateCount = beforeTemplates.size();
        int beforeUsersCount = beforeUsers.size();
        int beforeDataSourceCount = beforeDataSources.size();

        assertThat(beforeMatchCount).isGreaterThan(0);
        assertThat(beforeTemplateCount).isGreaterThan(0);
        assertThat(beforeUsersCount).isGreaterThan(0);
        assertThat(beforeDataSourceCount).isGreaterThan(0);

        logger.info(
                "Before backup, there are: {} Events, {} PatternKitTemplates, " +
                        "{} FileServerUsers, {} DataSources",
                beforeMatchCount, beforeTemplateCount, beforeUsersCount, beforeDataSourceCount);

        // when
        final Path databaseDump = databaseMgmtSvc.createDatabaseDump();
        logger.info("Created database dump at: {}", databaseDump);
        cleanupPaths.add(databaseDump);

        logger.info("Installing database...");
        databaseMgmtSvc.installDatabase(databaseDump);
        logger.info("Database successfully installed.");

        // then
        List<Match> afterMatches = eventService.fetchAll();
        List<PatternKitTemplate> afterTemplates = templateService.fetchAll();
        List<FileServerUser> afterUsers = userService.getAllUsers();
        List<DataSource<?>> afterDataSources = dataSourceService.fetchAll();

        int afterMatchCount = afterMatches.size();
        int afterTemplateCount = afterTemplates.size();
        int afterUsersCount = afterUsers.size();
        int afterDataSourceCount = afterDataSources.size();

        logger.info(
                "After re-install, there are: {} Events, {} PatternKitTemplates, " +
                        "{} FileServerUsers, {} DataSources",
                afterMatchCount, afterTemplateCount, afterUsersCount, afterDataSourceCount);

        assertThat(afterMatchCount).isEqualTo(beforeMatchCount);
        assertThat(afterTemplateCount).isEqualTo(beforeTemplateCount);
        assertThat(afterUsersCount).isEqualTo(beforeUsersCount);
        assertThat(afterDataSourceCount).isEqualTo(beforeDataSourceCount);
    }
}