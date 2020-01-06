/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.db.MatchRepository;
import self.me.matchday.model.Match;
import self.me.matchday.model.Match.MatchId;

@RestController
public class MatchController {

  private static final String LOG_TAG = "MatchController";


  private final MatchRepository matchRepository;

  public MatchController(MatchRepository matchRepository) {
    this.matchRepository = matchRepository;
  }

  @GetMapping("/matches/")
//  @ResponseBody
  public List<Match> fetchMatches() {
    final List<Match> matches = matchRepository.findAll();
    // Sort by date (descending)
    matches.sort((match, t1) -> (match.getDate().compareTo(t1.getDate())) * -1);
    return matches;
  }

  @GetMapping("/matches/{matchId}")
  Match fetchMatch(@PathVariable MatchId matchId ) {
    return matchRepository.findById(matchId)
        .orElseThrow(() -> new RuntimeException("Match not found with ID: " + matchId));
  }

  @PostMapping("/matches/")
  Match postMatch(@RequestBody Match match) {
    return matchRepository.save(match);
  }
}
