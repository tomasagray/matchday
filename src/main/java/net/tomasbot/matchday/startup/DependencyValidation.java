package net.tomasbot.matchday.startup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import net.tomasbot.matchday.api.service.admin.MySqlDumpWrapper;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import net.tomasbot.matchday.util.TelnetClientWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DependencyValidation implements CommandLineRunner {

  private static final Logger logger = LogManager.getLogger(DependencyValidation.class);
  private static final Random R = new Random();
  private static final Pattern versionPattern = Pattern.compile("[\\w\\s.]+");
  private static final List<File> cleanupDirs = new ArrayList<>();

  private final FFmpegPlugin ffmpegPlugin;
  private final MySqlDumpWrapper mysqldump;
  private final TelnetClientWrapper telnet;

  @Value("${DATA_ROOT}")
  private Path dataRoot;

  @Value("${CONFIG_ROOT}")
  private Path configRoot;

  public DependencyValidation(
      FFmpegPlugin ffmpegPlugin, MySqlDumpWrapper mysqldump, TelnetClientWrapper telnet) {
    this.ffmpegPlugin = ffmpegPlugin;
    this.mysqldump = mysqldump;
    this.telnet = telnet;
  }

  private static void dependencyError(@NotNull String dependency) throws IOException {
    throw new IOException("Could not determine version of required dependency: " + dependency);
  }

  @Override
  public void run(String... args) throws Exception {
    logger.info("Performing startup validation checks ...");

    validateStorage();
    validateFFmpeg();
    validateMysqldump();
    validateTelnetClient();

    cleanup();

    logger.info("Startup checks passed, proceeding with launch ...");
  }

  private void cleanup() throws IOException {
    logger.info("Cleaning up validation data ...");

    for (File testDir : cleanupDirs) {
      boolean deleted = testDir.delete();
      if (!deleted || testDir.exists())
        throw new IOException("Could not delete validation data! " + testDir);
    }
  }

  private void validateStorage() throws IOException {
    logger.info("Validating application storage directories ...");

    // ensure existence of required dirs
    if (!dataRoot.toFile().exists())
      throw new IOException("Could not find data storage directory: " + dataRoot);
    if (!configRoot.toFile().exists())
      throw new IOException("Could not find configuration directory: " + configRoot);

    // ensure required dirs are writable
    File dataTestDir = dataRoot.resolve("__TMP__" + R.nextInt()).toFile();
    File configTestDir = configRoot.resolve("__TMP__" + R.nextInt()).toFile();
    dataTestDir.deleteOnExit();
    configTestDir.deleteOnExit();
    cleanupDirs.add(dataTestDir);
    cleanupDirs.add(configTestDir);

    for (File testStorageDir : cleanupDirs) {
      Files.createDirectories(testStorageDir.toPath());
      if (!testStorageDir.exists())
        throw new IOException("Storage directory is not writable: " + testStorageDir);
    }
  }

  private void validateFFmpeg() throws IOException {
    final String ffmpegVersion = ffmpegPlugin.getFFmpegVersion();
    final String ffprobeVersion = ffmpegPlugin.getFFprobeVersion();

    logger.info("Found FFmpeg version: {}", ffmpegVersion);
    logger.info("Found FFprobe version: {}", ffprobeVersion);

    if (!versionPattern.matcher(ffmpegVersion).find()) dependencyError("ffmpeg");

    if (!versionPattern.matcher(ffprobeVersion).find()) dependencyError("ffprobe");
  }

  private void validateMysqldump() throws IOException {
    final String version = mysqldump.getVersion();
    logger.info("Found mysqldump version: {}", version);

    if (!versionPattern.matcher(version).find()) dependencyError("mysqldump");
  }

  private void validateTelnetClient() throws IOException {
    final String version = telnet.getVersion();
    logger.info("Found telnet client version: {}", version);

    if (!versionPattern.matcher(version).find()) dependencyError("telnet");
  }
}
