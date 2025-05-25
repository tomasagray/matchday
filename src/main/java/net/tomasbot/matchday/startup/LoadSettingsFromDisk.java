package net.tomasbot.matchday.startup;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.model.ApplicationSettings;

@Component
@Order(2)
public class LoadSettingsFromDisk implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(LoadSettingsFromDisk.class);
  private final SettingsService settingsService;

  public LoadSettingsFromDisk(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @Override
  public void run(String... args) {
    try {
      logger.info("Loading initial application settings from disk...");
      int loaded = settingsService.loadSettings();
      logger.info("... loaded {} application settings from disk.", loaded);
    } catch (IOException | InterruptedException e) {
      createSettingsFile();
    }
  }

  private void createSettingsFile() {
    try {
      logger.warn("Could not load application settings from disk; attempting to create...");
      ApplicationSettings settings = settingsService.getSettings();
      settingsService.updateSettings(settings);
      logger.info("Successfully created application settings file; proceeding...");
    } catch (Throwable e) {
      logger.error("Could not create application settings file!");
      throw new RuntimeException(e);
    }
  }
}
