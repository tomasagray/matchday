package net.tomasbot.matchday.api.service.admin;

import static net.tomasbot.matchday.config.settings.ArtworkStorageLocation.ARTWORK_LOCATION;
import static net.tomasbot.matchday.config.settings.BackupLocation.BACKUP_LOCATION;
import static net.tomasbot.matchday.config.settings.LogFilename.LOG_FILENAME;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.api.service.ZipService;
import net.tomasbot.matchday.db.RestorePointRepository;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.model.ArtworkSanityReport.DanglingArtwork;
import net.tomasbot.matchday.model.VideoSanityReport.DanglingLocatorPlaylist;
import net.tomasbot.matchday.model.VideoSanityReport.DanglingVideoStreamLocator;
import net.tomasbot.matchday.util.RecursiveDirectoryDeleter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackupService {

  private static final String ERROR_MSG = "Could not create backup: ";
  private static final String BACKUP_PREFIX = "matchday_";
  private static final String ARCHIVE_NAME = BACKUP_PREFIX + "backup_%s.zip";
  private static final String LOG_PATH = "log";
  private static final String ARTWORK_PATH = "artwork";

  private final DatabaseManagementService databaseService;
  private final SanityCheckService sanityCheckService;
  private final HydrationService hydrationService;
  private final ZipService zipService;
  private final SettingsService settingsService;
  private final RestorePointRepository restorePointRepository;

  public BackupService(
      DatabaseManagementService databaseService,
      SanityCheckService sanityCheckService,
      HydrationService hydrationService,
      ZipService zipService,
      SettingsService settingsService,
      RestorePointRepository restorePointRepository) {
    this.databaseService = databaseService;
    this.sanityCheckService = sanityCheckService;
    this.hydrationService = hydrationService;
    this.zipService = zipService;
    this.settingsService = settingsService;
    this.restorePointRepository = restorePointRepository;
  }

  private static void analyzeReport(@NotNull SanityReport report) {
    ArtworkSanityReport artworkReport = report.getArtworkSanityReport();

    List<Path> danglingArtwork = artworkReport.getDanglingFiles().stream().map(Path::of).toList();
    if (!danglingArtwork.isEmpty()) {
      fail("Found dangling Artwork: " + danglingArtwork);
    }

    List<DanglingArtwork> danglingDbEntries = artworkReport.getDanglingDbEntries();
    if (!danglingDbEntries.isEmpty()) {
      fail("Found dangling Artwork DB entries: " + danglingDbEntries);
    }

    VideoSanityReport videoSanityReport = report.getVideoSanityReport();
    List<DanglingLocatorPlaylist> danglingPlaylists = videoSanityReport.getDanglingPlaylists();
    if (!danglingPlaylists.isEmpty()) {
      fail("Found dangling VideoStreamLocatorPlaylists: " + danglingPlaylists);
    }

    List<? extends DanglingVideoStreamLocator> danglingLocators =
        videoSanityReport.getDanglingStreamLocators();
    if (!danglingLocators.isEmpty()) {
      fail("Found dangling VideoStreamLocators: " + danglingLocators);
    }
  }

  private static void copy(Path source, Path destination) throws IOException {
    try (final Stream<Path> walker = Files.walk(source)) {
      walker.forEach(
          file -> {
            try {
              Path resolved = destination.resolve(source.relativize(file));
              Files.copy(file, resolved, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    }
  }

  private static void deleteDatabaseDump(@NotNull Path databaseDump) throws IOException {
    File dumpFile = databaseDump.toFile();
    boolean deleted = dumpFile.delete();
    if (!deleted) {
      throw new IOException("Could not delete SQL dump file: " + dumpFile);
    }
  }

  private static void fail(@NotNull String msg) {
    throw new RuntimeException(ERROR_MSG + msg);
  }

  private static @NotNull Path findDumpFile(@NotNull Path searchDir) throws IOException {
    File[] dumpFiles = searchDir.toFile().listFiles((dir, name) -> name.endsWith(".sql"));
    if (dumpFiles == null || dumpFiles.length == 0) {
      throw new IOException("Could not find database dump file");
    }
    return dumpFiles[0].toPath();
  }

  private static void installData(@NotNull Path tmp, Path artworkLocation, Path logLocation)
      throws IOException {
    Path tmpArtwork = tmp.resolve(ARTWORK_PATH);
    Path tmpLog = tmp.resolve(LOG_PATH);
    if (tmpArtwork.toFile().exists()) copy(tmpArtwork, artworkLocation);
    if (tmpLog.toFile().exists()) copy(tmpLog, logLocation);
  }

  @NotNull
  private Path getArchivePath() {
    Path backupLocation = settingsService.getSetting(BACKUP_LOCATION, Path.class);
    long timestamp = Instant.now().getEpochSecond();
    return backupLocation.resolve(String.format(ARCHIVE_NAME, timestamp));
  }

  public RestorePoint createRestorePoint() throws IOException {
    checkSanity();
    Path backupArchive = createBackup();
    if (backupArchive == null || !(backupArchive.toFile().exists())) {
      throw new IOException("Could not create backup");
    }

    HydrationService.SystemImage systemImage = hydrationService.createSystemImage();
    RestorePoint restorePoint =
        RestorePoint.builder()
            .backupArchive(backupArchive)
            .timestamp(Timestamp.from(Instant.now()))
            .filesize(backupArchive.toFile().length())
            .eventCount(systemImage.getEvents().size())
            .competitionCount(systemImage.getCompetitions().size())
            .teamCount(systemImage.getTeams().size())
            .dataSourceCount(systemImage.getDataSources().size())
            .fileServerUserCount(systemImage.getFileServerUsers().size())
            .build();
    return restorePointRepository.save(restorePoint);
  }

  public List<RestorePoint> fetchAllRestorePoints() {
    return restorePointRepository.findAll();
  }

  private void checkSanity() {
    SanityReport report = sanityCheckService.createSanityReport();
    analyzeReport(report);
  }

  public RestorePoint restoreSystem(@NotNull UUID restorePointId) throws IOException, SQLException {

    Optional<RestorePoint> restoreOptional = restorePointRepository.findById(restorePointId);
    if (restoreOptional.isPresent()) {
      RestorePoint restorePoint = restoreOptional.get();
      createRestorePoint();
      loadBackupArchive(restorePoint.getBackupArchive());
      checkSanity();
      return restorePoint;
    } else {
      throw new IllegalArgumentException("No RestorePoint found with ID: " + restoreOptional);
    }
  }

  public Path createBackup() throws IOException {
    // get settings
    Path artworkStorageLocation = settingsService.getSetting(ARTWORK_LOCATION, Path.class);
    Path logPath = getLogLocation();
    Path archive = getArchivePath();

    Path tmp = Files.createTempDirectory(BACKUP_PREFIX);
    copy(artworkStorageLocation, tmp.resolve(ARTWORK_PATH));
    copy(logPath, tmp.resolve(LOG_PATH));
    Path databaseDump = databaseService.createDatabaseDump();
    Files.copy(databaseDump, tmp.resolve(databaseDump.getFileName()));
    zipService.zipFiles(archive.toFile(), tmp, tmp.toFile());

    deleteDatabaseDump(databaseDump);
    Files.walkFileTree(tmp, new RecursiveDirectoryDeleter());
    return archive;
  }

  public void loadBackupArchive(@NotNull Path archive) throws IOException, SQLException {
    Path artworkLocation = settingsService.getSetting(ARTWORK_LOCATION, Path.class);
    Path logLocation = getLogLocation();

    removeOldData(artworkLocation, logLocation);
    Path tmp = unzipBackup(archive);
    installData(tmp, artworkLocation, logLocation);
    // install database
    Path dumpFile = findDumpFile(tmp);
    databaseService.installDatabase(dumpFile);
    // remove temporary files
    Files.walkFileTree(tmp, new RecursiveDirectoryDeleter());
  }

  private Path getLogLocation() {
    return settingsService.getSetting(LOG_FILENAME, Path.class).getParent();
  }

  @NotNull
  private Path unzipBackup(@NotNull Path archive) throws IOException {
    Path tmp = Files.createTempDirectory(BACKUP_PREFIX);
    zipService.unzipArchive(archive.toFile(), tmp.toFile());
    return tmp;
  }

  private void removeOldData(Path artworkLocation, Path logLocation) throws IOException {
    Files.walkFileTree(artworkLocation, new RecursiveDirectoryDeleter());
    Files.walkFileTree(logLocation, new RecursiveDirectoryDeleter());
    ensureDirExists(artworkLocation);
    ensureDirExists(logLocation);
  }

  private void ensureDirExists(@NotNull Path dir) throws IOException {
    File file = dir.toFile();
    if (!file.exists()) {
      if (!file.mkdirs()) {
        throw new IOException("Could not create required directory: " + dir);
      }
    }
  }

  public byte[] readBackupArchive(@NotNull UUID restorePointId) throws IOException {
    Optional<RestorePoint> restorePointOptional = restorePointRepository.findById(restorePointId);
    if (restorePointOptional.isPresent()) {
      RestorePoint restorePoint = restorePointOptional.get();
      Path archive = restorePoint.getBackupArchive();
      return Files.readAllBytes(archive);
    }
    String msg = "Cannot download archive for non-existent RestorePoint: " + restorePointId;
    throw new IllegalArgumentException(msg);
  }

  @Transactional
  public Optional<RestorePoint> deleteRestorePoint(@NotNull UUID restorePointId)
      throws IOException {
    Optional<RestorePoint> rpOpt = restorePointRepository.findById(restorePointId);
    if (rpOpt.isPresent()) {
      RestorePoint restorePoint = rpOpt.get();
      File archive = restorePoint.getBackupArchive().toFile();
      restorePointRepository.delete(restorePoint);
      boolean deleted = archive.delete();
      if (!deleted || archive.exists()) {
        String msg = "Could not delete System Restore Point archive at: " + archive;
        throw new IOException(msg);
      }
    }
    return rpOpt;
  }

  public Path dehydrateToDisk() throws IOException {
    checkSanity();
    Path backupLocation = settingsService.getSetting(BACKUP_LOCATION, Path.class);
    return hydrationService.dehydrate(backupLocation);
  }

  public HydrationService.SystemImage dehydrate() {
    return hydrationService.dehydrate();
  }

  public void rehydrateFrom(Path json) throws IOException {
    hydrationService.rehydrate(json);
    checkSanity();
  }

  public void rehydrateFrom(HydrationService.SystemImage systemImage) {
    hydrationService.rehydrate(systemImage);
    checkSanity();
  }
}
