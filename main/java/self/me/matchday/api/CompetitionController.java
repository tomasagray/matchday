/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Competition;

@RestController
public class CompetitionController {

  private final CompetitionRepository repository;

  public CompetitionController(CompetitionRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/competitions")
  public List<Competition> fetchAllCompetitions() {
    return repository.findAll();
  }
}
