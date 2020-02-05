/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.db.CompetitionRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.util.Log;

@RestController
public class CompetitionController {

  private static final String LOG_TAG = "CompetitionController";

  private final CompetitionRepository repository;

  public CompetitionController(CompetitionRepository repository) {
    this.repository = repository;
  }

  /**
   * Fetch all Competitions in the database.
   *
   * @return All Competitions as an HttpEntity
   */
  @GetMapping("/competitions")
  public HttpEntity<List<CompetitionResource>> fetchAllCompetitions() {

    Log.i(LOG_TAG, "Retrieving all Competitions.");
    // Return container
    final List<CompetitionResource> competitionResources = new ArrayList<>();
    final List<Competition> competitions = repository.findAll();
    if (competitions.size() > 0) {
      competitions.forEach(
          competition -> {
            final CompetitionResource competitionResource = new CompetitionResource(competition);
            // Add a link to the Competition resource
            competitionResource.add(
                linkTo(
                        methodOn(CompetitionController.class)
                            .fetchCompetitionById(competition.getCompetitionId()))
                    .withSelfRel());
            competitionResources.add(competitionResource);
          });

      return new ResponseEntity<>(competitionResources, HttpStatus.OK);
    } else {
      Log.i(LOG_TAG, "Attempted to fetch all Competitions, but none returned");
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Retrieve a single Competition from the local database.
   *
   * @param competitionId MD5 String ID for the desired Competition.
   * @return A Competition Resource
   */
  @GetMapping("/competitions/competition/{competitionId}")
  public HttpEntity<CompetitionResource> fetchCompetitionById(
      @PathVariable final String competitionId) {

    // Log access attempt
    Log.i(LOG_TAG, "Attempting to retrieve Competition by ID: " + competitionId);
    final Optional<Competition> competitionOptional = repository.findById(competitionId);

    if (competitionOptional.isPresent()) {
      final Competition competition = competitionOptional.get();
      final CompetitionResource competitionResource = new CompetitionResource(competition);
      competitionResource.add(
          linkTo(methodOn(CompetitionController.class).fetchCompetitionById(competitionId))
              .withSelfRel());
      return new ResponseEntity<>(competitionResource, HttpStatus.OK);

    } else {
      Log.d(LOG_TAG, "Could not find any Competition using ID: " + competitionId);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }
}
