/*
 * Copyright (c) 2022.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.tomasbot.matchday.unit.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.tomasbot.matchday.TestDataCreator;
import net.tomasbot.matchday.api.resource.TeamResource;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.Match;
import net.tomasbot.matchday.model.ProperName;
import net.tomasbot.matchday.model.Team;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Team REST controller integration test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TeamControllerTest {

  private static final Logger logger = LogManager.getLogger(TeamControllerTest.class);
  private static final String NAME = "TeamControllerTest ";
  private static final List<Event> testMatches = new ArrayList<>();
  private static List<Team> testTeams;
  private static TestDataCreator testDataCreator;
  @LocalServerPort private int port;
  @Autowired private TestRestTemplate restTemplate;

  @BeforeAll
  static void setup(@Autowired TestDataCreator testDataCreator) {
    TeamControllerTest.testDataCreator = testDataCreator;
    TeamControllerTest.testTeams =
        IntStream.range(0, 10)
            .mapToObj(
                i -> {
                  final Match testMatch = testDataCreator.createTestMatch(NAME + i);
                  testMatches.add(testMatch);
                  return testMatch;
                })
            .map(Match::getHomeTeam)
            .collect(Collectors.toList());
  }

  @AfterAll
  static void tearDown() throws IOException {
    testMatches.forEach(
        event -> {
          logger.info("Deleting test Match: {}", event);
          testDataCreator.deleteTestEvent(event);
        });
    TestDataCreator.deleteGeneratedMatchArtwork(testMatches);
  }

  private Stream<Arguments> getAllTeamsArgs() {
    final String url = "http://localhost:" + port + "/api/v1/teams/";
    logger.info("Fetching all Teams from: {}", url);

    final ResponseEntity<CollectionModel<TeamResource>> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    final CollectionModel<TeamResource> body = response.getBody();
    assertThat(body).isNotNull();
    return body.getContent().stream().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("getAllTeamsArgs")
  @DisplayName("Validate retrieval of all Teams via REST controller")
  void fetchAllTeams(@NotNull TeamResource resource) {
    logger.info("Testing TeamResource: {}", resource);
    assertThat(resource.getId()).isNotNull();
    final ProperName properName = resource.getName();
    assertThat(properName).isNotNull();
    assertThat(properName.getName()).isNotNull().isNotEmpty();
  }

  private Stream<Arguments> getTeamByNameArgs() {
    return testTeams.stream().map(Arguments::of);
  }

  private ResponseEntity<TeamResource> getTeam(@NotNull UUID teamId) {
    final String url = String.format("http://localhost:%d/api/v1/teams/team/%s", port, teamId);
    return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
  }

  @ParameterizedTest
  @MethodSource("getTeamByNameArgs")
  @DisplayName("Validate retrieval of Team from database via REST controller by name")
  void fetchTeamByName(@NotNull Team team) {
    final UUID teamName = team.getId();
    final ResponseEntity<TeamResource> response = getTeam(teamName);
    logger.info("Got Team: {}", response);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    final TeamResource teamResource = response.getBody();
    logger.info("Got TeamResource: {}", teamResource);
    assertThat(teamResource).isNotNull();
    assertThat(teamResource.getId()).isEqualTo(teamName);
  }

  @ParameterizedTest
  @MethodSource("getTeamByNameArgs")
  @DisplayName("Validate retrieval of Events related to Team specified by {0} via REST controller")
  void fetchEventsForTeam(@NotNull Team team) {
    final UUID teamId = team.getId();
    final String url = String.format("http://localhost:%d/api/v1/teams/team/%s", port, teamId);
    logger.info("Getting Events for Team: {} @ URL: {}", teamId, url);

    final ResponseEntity<CollectionModel<Match>> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    logger.info("Got response: {}", response);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    final CollectionModel<Match> body = response.getBody();
    assertThat(body).isNotNull();
    final Collection<Match> events = body.getContent();
    events.forEach(
        event -> {
          logger.info("Got Event: {}", event);
          assertThat(event).isNotNull();
          assertThat(event.getHomeTeam()).isEqualTo(team);
        });
  }
}
