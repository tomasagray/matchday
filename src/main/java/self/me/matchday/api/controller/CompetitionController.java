/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api.controller;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.util.Log;

@RestController
public class CompetitionController {

  private static final String LOG_TAG = "CompetitionController";

  private final CompetitionService competitionService;

  @Autowired
  public CompetitionController(CompetitionService competitionService) {
    this.competitionService = competitionService;
  }


  /**
   * Provide all Competitions to the API.
   *
   * @return All Competitions as an HttpEntity.
   */
  @GetMapping("/competitions")
  public ResponseEntity<CollectionModel<CompetitionResource>> fetchAllCompetitions() {
    return
        competitionService
            .fetchAllCompetitions()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieve a single Competition from the local database.
   *
   * @param competitionId MD5 String ID for the desired Competition.
   * @return A Competition Resource.
   */
  @GetMapping("/competitions/competition/{competitionId}")
  public ResponseEntity<CompetitionResource> fetchCompetitionById(
      @PathVariable final String competitionId) {

    return
        competitionService
            .fetchCompetitionById(competitionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/competitions/competition/{competitionId}/emblem")
  public ResponseEntity<URL> fetchCompetitionEmblemUrl(@PathVariable final String competitionId) {

    Log.i(LOG_TAG, "Getting emblem for competition: " + competitionId);

    // TODO: implement competition emblem service
    URL url = null;
    try {
      url = new URL("http://www.competition-emblem-url.com");
    } catch (MalformedURLException ignored) {
    }

    return new ResponseEntity<>(url, HttpStatus.OK);
  }
}
