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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import self.me.matchday.TestDataCreator;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.EventsResource;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.model.Competition;
import self.me.matchday.model.Event;
import self.me.matchday.model.ProperName;
import self.me.matchday.plugin.datasource.parsing.fabric.Bolt;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("Competition REST controller integration test")
class CompetitionControllerTest {

  private static final Logger logger = LogManager.getLogger(CompetitionControllerTest.class);

  @LocalServerPort private int port;
  @Autowired private TestRestTemplate restTemplate;

  private static final String NAME = "Competition Controller Test ";
  private static List<Competition> testCompetitions;
  private static final List<Event> testMatches = new ArrayList<>();
  private static TestDataCreator testDataCreator;

  @BeforeAll
  static void setup(@Autowired @NotNull TestDataCreator testDataCreator) {
    CompetitionControllerTest.testDataCreator = testDataCreator;
    CompetitionControllerTest.testCompetitions =
        IntStream.range(0, 10)
            .mapToObj(
                i -> {
                  final Event testMatch = testDataCreator.createTestMatch(NAME + i);
                  testMatches.add(testMatch);
                  return testMatch;
                })
            .map(Event::getCompetition)
            .collect(Collectors.toList());
  }

  @AfterAll
  static void teardown() {
    testMatches.forEach(
        match -> {
          logger.info("Deleting test Match: {}", match);
          testDataCreator.deleteTestEvent(match);
        });
  }

  private Stream<Arguments> getAllTestCompetitionsArgs() {

    final String url = "http://localhost:" + port + "/competitions/";
    logger.info("Fetching all Competitions from: {}", url);
    final ResponseEntity<CollectionModel<CompetitionResource>> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    final CollectionModel<CompetitionResource> body = response.getBody();
    assertThat(body).isNotNull();

    final Stream<String> names =
        body.getContent().stream().map(CompetitionResource::getName).map(ProperName::getName);
    return Bolt.of(names)
        .zipWithBecoming(
            testCompetitions.stream(),
            (competition, name) -> Arguments.of(competition.getName().getName(), name))
        .stream();
  }

  @ParameterizedTest
  @MethodSource("getAllTestCompetitionsArgs")
  @DisplayName("Validate REST controller retrieval of all Competitions")
  void fetchAllCompetitions(String actual, String expected) {
    logger.info("Testing name: {}", actual);
    assertThat(actual).isEqualTo(expected);
  }

  private Stream<Arguments> getTestCompetitions() {
    return testCompetitions.stream().map(Arguments::of);
  }

  private ResponseEntity<CompetitionResource> getCompetition(@NotNull UUID competitionId) {
    final String url =
        String.format("http://localhost:%d/competitions/competition/%s", port, competitionId);
    logger.info("Getting data from URL: {}", url);
    return restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
  }

  @ParameterizedTest
  @MethodSource("getTestCompetitions")
  @DisplayName("Validate retrieving a Competition from the database by name")
  void fetchCompetitionByName(@NotNull Competition competition) {

    final UUID competitionId = competition.getId();
    logger.info("Testing with Competition Name: {}", competitionId);
    final ResponseEntity<CompetitionResource> response = getCompetition(competitionId);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

    final CompetitionResource body = response.getBody();
    assertThat(body).isNotNull();
    logger.info("Got response: {}", body);
    assertThat(body.getId()).isEqualTo(competitionId);
  }

  @ParameterizedTest
  @MethodSource("getTestCompetitions")
  @DisplayName("Validate retrieval of Teams by Competition name")
  void fetchCompetitionTeams(@NotNull Competition competition) {

    final String url =
        String.format(
            "http://localhost:%d/competitions/competition/%s/teams", port, competition.getId());
    logger.info("Getting teams from database for Competition: {}", competition);
    logger.info("Using URL: {}", url);

    final ResponseEntity<CollectionModel<TeamResource>> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    final CollectionModel<TeamResource> body = response.getBody();
    logger.info("Got response: {}", body);

    assertThat(body).isNotNull();
    final Collection<TeamResource> content = body.getContent();
    assertThat(content.size()).isNotZero();
    content.forEach(
        teamResource -> {
          logger.info("Team resource: {}", teamResource);
          assertThat(teamResource).isNotNull();
        });
  }

  @ParameterizedTest
  @MethodSource("getTestCompetitions")
  @DisplayName("Validate retrieval of Events associated with a given Competition")
  void fetchCompetitionEvents(@NotNull Competition competition) {

    final String url =
        String.format(
            "http://localhost:%d/competitions/competition/%s/events", port, competition.getId());
    final ResponseEntity<EventsResource> response =
        restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    final EventsResource body = response.getBody();
    logger.info("Got response: {}", body);
    assertThat(body).isNotNull();

    final List<MatchResource> matchResources = body.getMatches();
    assertThat(matchResources.size()).isNotZero();
    matchResources.forEach(
        eventResource -> {
          logger.info("Got MatchResource: {}", eventResource);
          assertThat(eventResource).isNotNull();
        });
  }
}
