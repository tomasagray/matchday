/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.db.HighlightShowRepository;
import self.me.matchday.model.HighlightShow;

@RestController
public class HighlightShowController {

  private final HighlightShowRepository repository;

  public HighlightShowController(HighlightShowRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/highlight-shows")
  List<HighlightShow> fetchAllHighlightShows() {
    final List<HighlightShow> highlightShows = repository.findAll();
    // Sort by date (descending)
    highlightShows.sort((highlightShow, t1) ->
        (highlightShow.getDate().compareTo(t1.getDate())) * -1);
    return highlightShows;
  }
}
