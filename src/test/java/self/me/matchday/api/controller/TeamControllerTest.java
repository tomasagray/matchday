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

package self.me.matchday.api.controller;

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
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;
import self.me.matchday.plugin.datasource.parsing.fabric.Bolt;
import self.me.matchday.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Team REST controller integration test")
class TeamControllerTest {

  private static final String LOG_TAG = "TeamControllerTest";

  @LocalServerPort private int port;
  @Autowired private TestRestTemplate restTemplate;

  private static final String NAME = "Test Team ";
  private static List<Team> testTeams;
  private static final List<Event> testMatches = new ArrayList<>();
  private static TestDataCreator testDataCreator;

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
            .map(Event::getHomeTeam)
            .collect(Collectors.toList());
  }

  @AfterAll
  static void tearDown() {
    testMatches.forEach(
        event -> {
          Log.i(LOG_TAG, "Deleting test Match: " + event);
          testDataCreator.deleteTestEvent(event);
        });
  }

  private Stream<Arguments> getAllTeamsArgs() {

    final String url = "http://localhost:" + port + "/teams/";
    Log.i(LOG_TAG, "Fetching all Teams from: " + url);
    final ResponseEntity<CollectionModel<TeamResource>> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    final CollectionModel<TeamResource> body = response.getBody();
    assertThat(body).isNotNull();

    final Stream<UUID> ids = body.getContent().stream().map(TeamResource::getId);
    return Bolt.of(ids)
        .zipWithBecoming(testTeams.stream(), (team, id) -> Arguments.of(team.getTeamId(), id))
        .stream();
  }

  @ParameterizedTest
  @MethodSource("getAllTeamsArgs")
  @DisplayName("Validate retrieval of all Teams via REST controller")
  void fetchAllTeams(UUID actual, UUID expected) {
    Log.i(LOG_TAG, String.format("Actual Team name: %s; Expected Team name: %s", actual, expected));
    assertThat(actual).isEqualTo(expected);
  }

  private Stream<Arguments> getTeamByNameArgs() {
    return testTeams.stream().map(Arguments::of);
  }

  private ResponseEntity<TeamResource> getTeam(@NotNull UUID teamId) {
    final String url = String.format("http://localhost:%d/teams/team/%s", port, teamId);
    return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
  }

  @ParameterizedTest
  @MethodSource("getTeamByNameArgs")
  @DisplayName("Validate retrieval of Team from database via REST controller by name")
  void fetchTeamByName(@NotNull Team team) {

    final UUID teamName = team.getTeamId();
    final ResponseEntity<TeamResource> response = getTeam(teamName);
    Log.i(LOG_TAG, "Got response: " + response);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    final TeamResource teamResource = response.getBody();
    Log.i(LOG_TAG, "Got TeamResource: " + teamResource);
    assertThat(teamResource).isNotNull();
    assertThat(teamResource.getId()).isEqualTo(teamName);
  }

  @ParameterizedTest
  @MethodSource("getTeamByNameArgs")
  @DisplayName("Validate retrieval of Events related to Team specified by {0} via REST controller")
  void fetchEventsForTeam(@NotNull Team team) {

    final UUID teamId = team.getTeamId();
    final String url = String.format("http://localhost:%d/teams/team/%s", port, teamId);
    Log.i(LOG_TAG, String.format("Getting Events for Team: %s @ URL: %s", teamId, url));

    final ResponseEntity<CollectionModel<Event>> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    Log.i(LOG_TAG, "Got response: " + response);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    final CollectionModel<Event> body = response.getBody();
    assertThat(body).isNotNull();
    final Collection<Event> events = body.getContent();
    events.forEach(
        event -> {
          Log.i(LOG_TAG, "Got Event: " + event);
          assertThat(event).isNotNull();
          assertThat(event.getHomeTeam()).isEqualTo(team);
        });
  }
}
