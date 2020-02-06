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
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

@RestController
public class TeamController {

  private static final String LOG_TAG = "TeamController";

  private final TeamRepository teamRepository;

  public TeamController(final TeamRepository teamRepository) {
    this.teamRepository = teamRepository;
  }

  /**
   * Fetch all Teams from local DB
   * @return A List of Teams as an HttpEntity
   */
  @GetMapping("/teams")
  HttpEntity<List<TeamResource>> fetchAllTeams() {

    Log.i(LOG_TAG, "Fetching all Teams");

    // Result container
    final List<TeamResource> teamResources = new ArrayList<>();
    final List<Team> teams = teamRepository.findAll();
    if(teams.size() > 0) {
      teams.forEach(team -> {
        final TeamResource teamResource = new TeamResource(team);
        // add link
        teamResource.add(
            linkTo(methodOn(TeamController.class).fetchTeamById(team.getTeamId())).withSelfRel()
        );
        // add to result container
        teamResources.add(teamResource);
      });

      return new ResponseEntity<>(teamResources, HttpStatus.OK);

    } else {

      Log.d(LOG_TAG, "Attempted to fetch all Teams, but nothing was found.");
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Fetch a single Team from the local DB, specified by the Team ID.
   * @param teamId The Team ID (MD5 String)
   * @return The Team as an HttpEntity
   */
  @GetMapping("/teams/team/{teamId}")
  HttpEntity<TeamResource> fetchTeamById(@PathVariable final String teamId) {

    Log.i(LOG_TAG, "Fetching Team with ID: " + teamId);

    // Get Team from local DB
    final Optional<Team> teamOptional = teamRepository.findById(teamId);
    if(teamOptional.isPresent()) {
      final TeamResource teamResource = new TeamResource(teamOptional.get());
      // add link
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchTeamById(teamId)).withSelfRel()
      );

      return new ResponseEntity<>(teamResource, HttpStatus.OK);
    } else {

      Log.d(LOG_TAG, "Could not find Team with ID: " + teamId);
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }
}
