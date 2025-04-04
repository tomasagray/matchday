package net.tomasbot.matchday.api.service.admin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.tomasbot.matchday.api.service.ArtworkService;
import net.tomasbot.matchday.db.EventRepository;
import net.tomasbot.matchday.model.Artwork;
import net.tomasbot.matchday.model.ArtworkSanityReport;
import net.tomasbot.matchday.model.Event;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtworkHealingService {

  private final ArtworkSanityService sanityService;
  private final ArtworkService artworkService;
  private final EventRepository eventRepository;

  public ArtworkHealingService(
      ArtworkSanityService sanityService,
      ArtworkService artworkService,
      EventRepository eventRepository) {
    this.sanityService = sanityService;
    this.artworkService = artworkService;
    this.eventRepository = eventRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArtworkSanityReport autoHealArtwork(@NotNull ArtworkSanityReport report)
      throws IOException {
    List<ArtworkSanityReport.DanglingArtwork> danglingDbEntries = report.getDanglingDbEntries();

    for (ArtworkSanityReport.DanglingArtwork danglingArtwork : danglingDbEntries) {
      Optional<Artwork> artworkOptional =
          artworkService.fetchArtworkAt(Path.of(danglingArtwork.getFilePath()));
      if (artworkOptional.isPresent()) {
        Artwork artwork = artworkOptional.get();
        Optional<Event> eventOptional = eventRepository.findByArtwork(artwork);

        if (eventOptional.isPresent()) {
          Event event = eventOptional.get();
          event.setArtwork(null);
        }

        artworkService.deleteArtwork(artwork);
      }
    }

    List<Path> danglingFiles = report.getDanglingFiles().stream().map(Path::of).toList();
    deleteArtworkFiles(danglingFiles);

    return sanityService.createArtworkSanityReport();
  }

  private void deleteArtworkFiles(@NotNull List<Path> danglingFiles) throws IOException {
    for (Path art : danglingFiles) {
      File artFile = art.toFile();
      if (!artFile.exists()) continue;

      boolean deleted = artFile.delete();
      if (!deleted || artFile.exists()) {
        throw new IOException("Could not delete Artwork file at: " + artFile);
      }
    }
  }
}
