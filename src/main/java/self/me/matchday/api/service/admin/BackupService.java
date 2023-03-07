package self.me.matchday.api.service.admin;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.SanityCheckService;
import self.me.matchday.api.service.SanityCheckService.SanityReport;
import self.me.matchday.api.service.SettingsService;
import self.me.matchday.api.service.ZipService;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.Settings;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;
import self.me.matchday.util.RecursiveDirectoryDeleter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

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

    public BackupService(
            DatabaseManagementService databaseService,
            SanityCheckService sanityCheckService,
            HydrationService hydrationService, ZipService zipService,
            SettingsService settingsService) {
        this.databaseService = databaseService;
        this.sanityCheckService = sanityCheckService;
        this.hydrationService = hydrationService;
        this.zipService = zipService;
        this.settingsService = settingsService;
    }

    @NotNull
    private static Path getArchivePath(@NotNull Settings settings) {
        Path backupLocation = settings.getBackupLocation();
        long timestamp = Instant.now().getEpochSecond();
        return backupLocation.resolve(String.format(ARCHIVE_NAME, timestamp));
    }

    private static void analyzeReport(@NotNull SanityReport report) {

        SanityReport.ArtworkSanityReport artworkReport = report.getArtworkSanityReport();
        List<Path> danglingArtwork = artworkReport.getDanglingFiles();
        if (danglingArtwork.size() > 0) {
            fail( "Found dangling Artwork: " + danglingArtwork);
        }
        List<Artwork> danglingDbEntries = artworkReport.getDanglingDbEntries();
        if (danglingDbEntries.size() > 0) {
            fail( "Found dangling Artwork DB entries: " + danglingDbEntries);
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
        try(final Stream<Path> walker = Files.walk(source)) {
            walker.forEach(file -> {
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
        File[] dumpFiles =
                searchDir.toFile().listFiles((dir, name) -> name.endsWith(".sql"));
        if (dumpFiles == null || dumpFiles.length == 0) {
            throw new IOException("Could not find database dump file");
        }
        return dumpFiles[0].toPath();
    }

    public Path createBackup() throws IOException {

        // get settings
        Settings settings = settingsService.getSettings();
        Path artworkStorageLocation = settings.getArtworkStorageLocation();
        Path logPath = settings.getLogFilename().getParent();
        Path archive = getArchivePath(settings);

        checkSanity();
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

    public Path restoreFromBackup(@NotNull Path archive) throws IOException, SQLException {

        Settings settings = settingsService.getSettings();
        Path artworkLocation = settings.getArtworkStorageLocation();
        Path logLocation = settings.getLogFilename().getParent();

        Path backup = createBackup();
        if (backup == null || !(backup.toFile().exists())) {
            throw new IOException("Could not create backup");
        }

        Path tmp = Files.createTempDirectory("matchday_");
        zipService.unzipArchive(archive.toFile(), tmp.toFile());
        Files.walkFileTree(artworkLocation, new RecursiveDirectoryDeleter());
        Files.walkFileTree(logLocation, new RecursiveDirectoryDeleter());
        copy(tmp.resolve(LOG_PATH), logLocation);
        copy(tmp.resolve(ARTWORK_PATH), artworkLocation);
        Path dumpFile = findDumpFile(tmp);
        databaseService.installDatabase(dumpFile);
        checkSanity();

        Files.walkFileTree(tmp, new RecursiveDirectoryDeleter());
        return backup;
    }

    public Path dehydrate() throws IOException {
        checkSanity();
        Path backupLocation = settingsService.getSettings().getBackupLocation();
        return hydrationService.dehydrate(backupLocation);
    }

    public void rehydrate(Path json) throws IOException {
        hydrationService.rehydrate(json);
        checkSanity();
    }
}
