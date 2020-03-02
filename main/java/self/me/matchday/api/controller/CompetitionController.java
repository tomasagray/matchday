/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api.controller;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.util.Log;

@RestController
public class CompetitionController {

  private static final String LOG_TAG = "CompetitionController";

  @Autowired
  private CompetitionRepository competitionRepository;
  @Autowired
  private CompetitionResourceAssembler competitionResourceAssembler;


  /**
   * Fetch all Competitions in the database.
   *
   * @return All Competitions as an HttpEntity.
   */
  @GetMapping("/competitions")
  public ResponseEntity<CollectionModel<CompetitionResource>> fetchAllCompetitions() {

    Log.i(LOG_TAG, "Retrieving all Competitions.");
    final List<Competition> competitions = competitionRepository.findAll();
    if (competitions.size() > 0) {
      return new ResponseEntity<>(competitionResourceAssembler.toCollectionModel(competitions),
          HttpStatus.OK);
    } else {
      Log.i(LOG_TAG, "Attempted to fetch all Competitions, but none returned");
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
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

    // Log access attempt
    Log.i(LOG_TAG, "Attempting to retrieve Competition by ID: " + competitionId);
    return competitionRepository
        .findById(competitionId)
        .map(competitionResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/competitions/competition/{competitionId}/emblem")
  public ResponseEntity<URL> fetchCompetitionEmblemUrl(@PathVariable final String competitionId) {

    // TODO: implement this
    Log.i(LOG_TAG, "Getting emblem for competition: " + competitionId);

    URL url = null;
    try {
      url = new URL("http://www.competition-emblem-url.com");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return new ResponseEntity<>(url, HttpStatus.OK);
  }
}
