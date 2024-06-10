package self.me.matchday.unit.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.api.service.SettingsService;
import self.me.matchday.model.*;
import self.me.matchday.util.JsonParser;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation tests for settings persistence file service")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SettingsFileServiceTest {

  private static final Logger logger = LogManager.getLogger(SettingsFileServiceTest.class);

  private final SettingsService.SettingsFileService fileService;

  @Autowired
  SettingsFileServiceTest(SettingsService.SettingsFileService fileService) {
    this.fileService = fileService;
  }

  @BeforeEach
  void setup() throws IOException {
    File settingsFile = fileService.getSettingsFile().toFile();
    if (!settingsFile.exists()) {
      boolean created = settingsFile.createNewFile();
      if (!created) {
        throw new IOException("Could not create test settings file!");
      }
      logger.info("Created test settings file at: {}", settingsFile);
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test writing data to settings.json file")
  void testWriteSettingsFile() throws IOException, InterruptedException {
    // given
    final File settingsFile = fileService.getSettingsFile().toFile();
    ApplicationSettings settings = new ApplicationSettings();
    Path testSetting1 = Path.of("/some/path");
    Path testSetting2 = Path.of("/another/setting");
    Match testObjectGraph =
        Match.builder()
            .competition(new Competition("Settings.json"))
            .date(LocalDateTime.now().minusDays(3))
            .fixture(new Fixture(700))
            .build();

    // when
    settings.put(new Setting.GenericSetting<>(testSetting1, "Some setting"));
    settings.put(new Setting.GenericSetting<>(testSetting2, testObjectGraph));

    logger.info("Writing test settings:\n{}", JsonParser.toJson(settings));
    fileService.writeSettingsFile(settings);

    // then
    assertThat(settingsFile).exists();
    assertThat(settingsFile.length()).isGreaterThan(0);
    logger.info("Test file successfully written");
  }

  @Test
  @Order(2)
  @DisplayName("Test reading of the settings.json file")
  void testReadSettingsFile() throws IOException, InterruptedException {
    logger.info("Reading settings file: {}", fileService.getSettingsFile());
    ApplicationSettings settings = fileService.readSettingsFile();

    logger.info("Settings file successfully read.");
    logger.info("Settings are:\n{}", JsonParser.toJson(settings));

    assertThat(settings).isNotNull();
    assertThat(settings.size()).isGreaterThan(0);
  }
}
