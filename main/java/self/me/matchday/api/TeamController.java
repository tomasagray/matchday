/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Team;

@RestController
public class TeamController {

  private final TeamRepository teamRepository;

  public TeamController(final TeamRepository teamRepository) {
    this.teamRepository = teamRepository;
  }

  @GetMapping("/teams")
  List<Team> fetchAllTeams() {
    return teamRepository.findAll();
  }
}
