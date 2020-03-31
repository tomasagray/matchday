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
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.service.TeamService;

@RestController
public class TeamController {

  private static final String LOG_TAG = "TeamController";

  private final TeamService teamService;

  @Autowired
  public TeamController(TeamService teamService) {
    this.teamService = teamService;
  }

  /**
   * Publish all Teams to the API.
   *
   * @return A List of Teams as an HttpEntity.
   */
  @GetMapping("/teams")
  public ResponseEntity<CollectionModel<TeamResource>> fetchAllTeams() {

    return
        teamService
            .fetchAllTeams()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
  }

  /**
   * Publish a single Team to the API, specified by the Team ID.
   *
   * @param teamId The Team ID (MD5 String)
   * @return The Team as an HttpEntity.
   */
  @GetMapping("/teams/team/{teamId}")
  public ResponseEntity<TeamResource> fetchTeamById(@PathVariable final String teamId) {

    return
        teamService
            .fetchTeamById(teamId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/teams/team/{teamId}/emblem")
  public ResponseEntity<URL> fetchTeamEmblemUrl(@PathVariable final String teamId) {

    // TODO: implement Team artwork service

    URL url = null;
    try {
      url = new URL("http://www.team-emblem-url.com");
    } catch (MalformedURLException ignored) {
    }
    return new ResponseEntity<>(url, HttpStatus.OK);
  }
}
