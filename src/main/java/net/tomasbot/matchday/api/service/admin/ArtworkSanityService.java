package net.tomasbot.matchday.api.service.admin;

import static net.tomasbot.matchday.config.settings.ArtworkStorageLocation.ARTWORK_LOCATION;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.tomasbot.matchday.api.service.ArtworkService;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.model.Artwork;
import net.tomasbot.matchday.model.ArtworkSanityReport;
import net.tomasbot.matchday.model.ArtworkSanityReport.ArtworkSanityReportBuilder;
import net.tomasbot.matchday.model.ArtworkSanityReport.DanglingArtwork;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtworkSanityService {

  private final SettingsService settingsService;
  private final ArtworkService artworkService;

  public ArtworkSanityService(SettingsService settingsService, ArtworkService artworkService) {
    this.settingsService = settingsService;
    this.artworkService = artworkService;
  }

  @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
  public ArtworkSanityReport createArtworkSanityReport() {
    ArtworkSanityReportBuilder reportBuilder = ArtworkSanityReport.builder();

    int artworkCount = artworkService.fetchAllArtwork().size();
    reportBuilder.totalDbEntries(artworkCount);

    List<String> danglingFiles =
        findDanglingArtworkFiles(reportBuilder).stream().map(Path::toString).toList();
    reportBuilder.danglingFiles(danglingFiles);

    List<DanglingArtwork> danglingEntries = findDanglingEntries();
    reportBuilder.danglingDbEntries(danglingEntries);

    return reportBuilder.build();
  }

  /**
   * Finds files which reside in the Artwork storage path, but do not have a corresponding entry in
   * the database.
   *
   * @param reportBuilder An instance of a report builder
   * @return An updated report builder
   */
  private @NotNull List<Path> findDanglingArtworkFiles(ArtworkSanityReportBuilder reportBuilder) {
    final List<Path> danglingFiles = new ArrayList<>();

    // find all Artwork files
    final File storage = settingsService.getSetting(ARTWORK_LOCATION, Path.class).toFile();
    final File[] artworkFiles = storage.listFiles();

    if (artworkFiles != null) {
      // - save total files found
      reportBuilder.totalFiles(artworkFiles.length);
      for (final File file : artworkFiles) {
        final Path filepath = file.toPath();
        final Optional<Artwork> artwork = artworkService.fetchArtworkAt(filepath);
        if (artwork.isEmpty()) {
          // artwork not in DB
          danglingFiles.add(filepath);
        }
      }
    }

    return danglingFiles;
  }

  /**
   * Check if there are Artwork entries in the database which do not have a file on the filesystem
   *
   * @return The updated report builder
   */
  private @NotNull List<DanglingArtwork> findDanglingEntries() {
    final List<DanglingArtwork> danglingEntries = new ArrayList<>();
    final List<Artwork> artworks = artworkService.fetchAllArtwork();

    for (final Artwork artwork : artworks) {
      final Path artworkFile = artwork.getFile();
      if (artworkFile == null || !artworkFile.toFile().exists()) {
        danglingEntries.add(new DanglingArtwork(artwork));
      }
    }

    // - add dangling DB entries
    return danglingEntries;
  }
}
