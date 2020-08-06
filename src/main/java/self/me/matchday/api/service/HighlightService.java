package self.me.matchday.api.service;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.HighlightRepository;
import self.me.matchday.model.Event.EventSorter;
import self.me.matchday.model.Highlight;
import self.me.matchday.util.Log;

@Service
public class HighlightService {

  private static final String LOG_TAG = "HighlightService";
  private static final EventSorter EVENT_SORTER = new EventSorter();

  private final HighlightRepository highlightRepository;

  @Autowired
  public HighlightService(final HighlightRepository highlightRepository) {

    this.highlightRepository = highlightRepository;
  }

  /**
   * Retrieve all Highlight Shows from the database.
   *
   * @return Optional collection model of highlight show resources.
   */
  public Optional<List<Highlight>> fetchAllHighlights() {

    Log.i(LOG_TAG, "Fetching all Highlight Shows from the database.");
    // Retrieve highlights from database
    final List<Highlight> highlights = highlightRepository.findAll();

    if (highlights.size() > 0) {
      // Sort in reverse chronological order
      highlights.sort(EVENT_SORTER);
      // return DTO
      return Optional.of(highlights);
    } else {
      Log.d(LOG_TAG, "Attempting to retrieve all Highlight Shows, but none found");
      return Optional.empty();
    }
  }

  /**
   * Retrieve a specific Highlight from the database.
   *
   * @param highlightShowId ID of the Highlight Show.
   * @return The requested Highlight, or empty().
   */
  public Optional<Highlight> fetchHighlight(@NotNull String highlightShowId) {

    Log.i(LOG_TAG, "Fetching Highlight Show for ID: " + highlightShowId);
    return
        highlightRepository
            .findById(highlightShowId);
  }
}
