package self.me.matchday.unit.api.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.api.service.ZipService;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Validation tests for the ZipService")
class ZipServiceTest {

  private static final String TEST_DATA = "src/test/resources/data/zip/";
  private static final String OUTPUT_DIR = "/projectdata/matchday/test/zip-service/";

  private static final Logger logger = LogManager.getLogger(ZipServiceTest.class);

  private final ZipService zipService;

  @Autowired
  ZipServiceTest(ZipService zipService) {
    this.zipService = zipService;
  }

  @BeforeEach
  void setup() throws IOException {

    final File outputDir = new File(OUTPUT_DIR);
    if (!outputDir.exists()) {
      logger.info("Test output dir {} does not exist; creating...", outputDir);
      boolean created = outputDir.mkdirs();
      if (!created) {
        throw new IOException("Could not create test output dir: " + outputDir);
      }
    } else {
      logger.info("Test output dir {} exists, proceeding with test...", outputDir);
    }
  }

  @AfterEach
  void cleanup() throws IOException {

    logger.info("Cleaning up test output...");
    Files.walkFileTree(
        Path.of(OUTPUT_DIR),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            logger.info("Deleting file: {}", file);
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            logger.info("Deleting directory: {}", dir);
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  @Test
  @DisplayName("Validating zipping multiple files & directories")
  void testZipFilesAndDirs() throws IOException {

    // given
    final long expectedFileSize = 65_734;
    final String archiveName = OUTPUT_DIR + "zip-test-output.zip";

    // when
    logger.info("Zipping: {} to {}...", TEST_DATA, archiveName);
    zipService.zipFiles(new File(archiveName), null, new File(TEST_DATA));

    // then
    logger.info("Checking existence of archive at: {}", archiveName);
    final File archive = new File(archiveName);
    assertThat(archive.exists()).isTrue();

    final long actualFilesize = archive.length();
    logger.info("Size of archive: {} bytes", actualFilesize);
    assertThat(actualFilesize).isCloseTo(expectedFileSize, Offset.offset(10L));
  }

  @Test
  @DisplayName("Validate ability to unzip archive")
  void testUnzipArchive() throws IOException {

    // given
    testZipFilesAndDirs(); // create test archive
    final File archiveName = new File(OUTPUT_DIR + "zip-test-output.zip");
    final File unzipDir = new File(OUTPUT_DIR + File.separator + "unzip");
    final int expectedFileCount = 8;

    // when
    logger.info("Unzipping test archive: {} to {}", archiveName, unzipDir);
    zipService.unzipArchive(archiveName, unzipDir);

    logger.info("Checking existence of unzipped files...");
    final AtomicInteger count = new AtomicInteger(0);
    Files.walkFileTree(
        unzipDir.toPath(),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            logger.info("Found file: {}", file);
            count.incrementAndGet();
            return FileVisitResult.CONTINUE;
          }
        });

    // then
    int actualFileCount = count.get();
    logger.info("Found: {} files.", actualFileCount);
    assertThat(actualFileCount).isEqualTo(expectedFileCount);
  }
}
