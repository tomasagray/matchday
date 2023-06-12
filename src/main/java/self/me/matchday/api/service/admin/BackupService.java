package self.me.matchday.api.service.admin;

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
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.api.service.SettingsService;
import self.me.matchday.api.service.ZipService;
import self.me.matchday.db.RestorePointRepository;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.RestorePoint;
import self.me.matchday.model.SanityReport;
import self.me.matchday.model.Settings;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.util.RecursiveDirectoryDeleter;

@Service
public class BackupService {

  private static final String ERROR_MSG = "Could not create backup: ";
  private static final String ARCHIVE_NAME = "matchday_backup_%s.zip";
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

  @NotNull
  private static Path getArchivePath(@NotNull Settings settings) {
    Path backupLocation = settings.getBackupLocation();
    long timestamp = Instant.now().getEpochSecond();
    return backupLocation.resolve(String.format(ARCHIVE_NAME, timestamp));
  }

  private static void analyzeReport(@NotNull SanityReport report) {

    SanityReport.ArtworkSanityReport artworkReport = report.getArtworkSanityReport();
    List<String> danglingArtwork = artworkReport.getDanglingFiles();
    if (danglingArtwork.size() > 0) {
      fail("Found dangling Artwork: " + danglingArtwork);
    }
    List<Artwork> danglingDbEntries = artworkReport.getDanglingDbEntries();
    if (danglingDbEntries.size() > 0) {
      fail("Found dangling Artwork DB entries: " + danglingDbEntries);
    }
    SanityReport.VideoSanityReport videoSanityReport = report.getVideoSanityReport();
    List<VideoStreamLocatorPlaylist> danglingPlaylists = videoSanityReport.getDanglingPlaylists();
    if (danglingPlaylists.size() > 0) {
      fail("Found dangling VideoStreamLocatorPlaylists: " + danglingPlaylists);
    }
    List<VideoStreamLocator> danglingLocators = videoSanityReport.getDanglingStreamLocators();
    if (danglingLocators.size() > 0) {
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

  public Path createBackup() throws IOException {

    // get settings
    Settings settings = settingsService.getSettings();
    Path artworkStorageLocation = settings.getArtworkStorageLocation();
    Path logPath = settings.getLogFilename().getParent();
    Path archive = getArchivePath(settings);

    Path tmp = Files.createTempDirectory("matchday_");
    copy(artworkStorageLocation, tmp.resolve(ARTWORK_PATH));
    copy(logPath, tmp.resolve(LOG_PATH));
    Path databaseDump = databaseService.createDatabaseDump();
    Files.copy(databaseDump, tmp.resolve(databaseDump.getFileName()));
    zipService.zipFiles(archive.toFile(), tmp, tmp.toFile());

    deleteDatabaseDump(databaseDump);
    Files.walkFileTree(tmp, new RecursiveDirectoryDeleter());
    return archive;
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

  public void loadBackupArchive(@NotNull Path archive) throws IOException, SQLException {

    Settings settings = settingsService.getSettings();
    Path artworkLocation = settings.getArtworkStorageLocation();
    Path logLocation = settings.getLogFilename().getParent();

    Files.walkFileTree(artworkLocation, new RecursiveDirectoryDeleter());
    Files.walkFileTree(logLocation, new RecursiveDirectoryDeleter());

    Path tmp = Files.createTempDirectory("matchday_");
    zipService.unzipArchive(archive.toFile(), tmp.toFile());
    Path tmpArtwork = tmp.resolve(ARTWORK_PATH);
    Path tmpLog = tmp.resolve(LOG_PATH);
    if (tmpArtwork.toFile().exists()) copy(tmpArtwork, artworkLocation);
    if (tmpLog.toFile().exists()) copy(tmpLog, logLocation);
    Path dumpFile = findDumpFile(tmp);
    databaseService.installDatabase(dumpFile);

    Files.walkFileTree(tmp, new RecursiveDirectoryDeleter());
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
    Path backupLocation = settingsService.getSettings().getBackupLocation();
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
