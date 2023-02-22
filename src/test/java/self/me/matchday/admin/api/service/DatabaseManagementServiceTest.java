package self.me.matchday.unit.api.service.admin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.api.service.admin.DatabaseManagementService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

    @Autowired
    DatabaseManagementServiceTest(DatabaseManagementService databaseMgmtSvc) {
        this.databaseMgmtSvc = databaseMgmtSvc;
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
        assertThat(dumpFile.length()).isGreaterThan(0);

        cleanupPaths.add(dumpPath);
    }
}