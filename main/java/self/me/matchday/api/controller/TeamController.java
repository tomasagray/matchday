/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.db.TeamRepository;
import self.me.matchday.model.Team;
import self.me.matchday.util.Log;

@RestController
public class TeamController {

  private static final String LOG_TAG = "TeamController";

  @Autowired
  private TeamRepository teamRepository;
  @Autowired
  private TeamResourceAssembler teamResourceAssembler;

  /**
   * Fetch all Teams from local DB
   *
   * @return A List of Teams as an HttpEntity
   */
  @GetMapping("/teams")
  public ResponseEntity<CollectionModel<TeamResource>> fetchAllTeams() {

    Log.i(LOG_TAG, "Fetching all Teams");

    final List<Team> teams = teamRepository.findAll();
    if (teams.size() > 0) {
      return new ResponseEntity<>(teamResourceAssembler.toCollectionModel(teams), HttpStatus.OK);
    } else {
      Log.d(LOG_TAG, "Attempted to fetch all Teams, but nothing was found.");
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Fetch a single Team from the local DB, specified by the Team ID.
   *
   * @param teamId The Team ID (MD5 String)
   * @return The Team as an HttpEntity
   */
  @GetMapping("/teams/team/{teamId}")
  public ResponseEntity<TeamResource> fetchTeamById(@PathVariable final String teamId) {

    Log.i(LOG_TAG, "Fetching Team with ID: " + teamId);
    // Get Team from local DB
    final Optional<Team> teamOptional = teamRepository.findById(teamId);
    return teamOptional.map(teamResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/teams/team/{teamId}/emblem")
  public ResponseEntity<URL> fetchTeamEmblemUrl(@PathVariable final String teamId) {
    // todo: implement this

    URL url = null;
    try {
      url = new URL("http://www.team-emblem-url.com");
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    return new ResponseEntity<>(url, HttpStatus.OK);
  }
}
