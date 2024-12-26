package net.tomasbot.matchday.unit.plugin.datasource.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Stream;
import net.tomasbot.matchday.model.Fixture;
import net.tomasbot.matchday.plugin.datasource.parsing.type.FixtureHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Validate functionality of FixtureHandler text-to-Fixture parser")
class FixtureHandlerTest {

  private static final Logger logger = LogManager.getLogger(FixtureHandlerTest.class);
  private static final Random R = new Random();
  private final FixtureHandler fixtureHandler = new FixtureHandler();

  private static String randomizeString(@NotNull String str) {
    return str.chars()
        .mapToObj(i -> (char) i)
        .map(c -> R.nextInt() % 2 == 0 ? Character.toLowerCase(c) : Character.toUpperCase(c))
        .collect(
            Collector.of(
                StringBuilder::new,
                StringBuilder::append,
                StringBuilder::append,
                StringBuilder::toString));
  }

  @Test
  @DisplayName("Validate creation of Fixtures from integers")
  void testIntegerFixtures() {
    // given
    final int startFixture = 1;
    final int endFixture = 38;

    // when
    for (int i = startFixture; i <= endFixture; i++) {
      String expectedTitle = "Matchday " + i;
      Fixture fixture = createRoundRobinFixture(String.valueOf(i));
      logger.info("Testing Fixture #{}: {}", i, fixture);

      // then
      assertThat(fixture.getTitle()).isEqualTo(expectedTitle);
      assertThat(fixture.getFixtureNumber()).isEqualTo(i);
    }
  }

  @Test
  @DisplayName("Validate regular season fixtures (e.g., Matchday 23)")
  void testSeasonFixtures() {
    // given
    final int startFixture = 1;
    final int endFixture = 38;

    // when
    for (int i = startFixture; i <= endFixture; i++) {
      String expectedTitle = "Matchday " + i;
      Fixture fixture = createRoundRobinFixture(expectedTitle);
      logger.info("Testing Fixture: {}", fixture);

      // then
      assertThat(fixture.getTitle()).isEqualTo(expectedTitle);
      assertThat(fixture.getFixtureNumber()).isEqualTo(i);
    }
  }

  @Test
  @DisplayName("Validate creation of Round Robin Fixtures")
  void testRoundRobin() {
    // given
    final String roundOf16Data = "Round of 16";
    final String roundOf32Data = "Round of 32";
    final String roundOf64Data = "Round of 64";
    final int seed = R.nextInt(1, 1_000);
    final String randomRoundData = "      Round     of        " + seed;

    // when
    Fixture roundOf16 = createRoundRobinFixture(roundOf16Data);
    Fixture roundOf32 = createRoundRobinFixture(roundOf32Data);
    Fixture roundOf64 = createRoundRobinFixture(roundOf64Data);
    Fixture randomRound = createRoundRobinFixture(randomRoundData);

    // then
    assertThat(roundOf16).isEqualTo(Fixture.RoundOf16);
    assertThat(roundOf32).isEqualTo(Fixture.RoundOf32);
    assertThat(roundOf64).isEqualTo(Fixture.RoundOf64);
    assertThat(randomRound).isEqualTo(new Fixture("Round of " + seed, 1024 + seed));
  }

  private Fixture createRoundRobinFixture(@NotNull String data) {
    logger.info("Creating fixture with data: {} ...", data);
    Fixture fixture = fixtureHandler.getHandler().apply(data);
    logger.info("Created round robin Fixture: {}", fixture);
    return fixture;
  }

  @Test
  @DisplayName("Validate creation of Group Stage Fixtures")
  void testGroupStage() {
    List<String> testStrings =
        Stream.of("Group Stage", "  Group     Stage    ", " group stage    ")
            .map(FixtureHandlerTest::randomizeString)
            .toList();

    performTournamentTest(testStrings, Fixture.GroupStage);
  }

  @Test
  @DisplayName("Validate creation of Quarter-final Fixtures")
  void testQuarterFinal() {
    // given
    List<String> testStrings =
        Stream.of("Quarter final", "Quarter-final", "Quarterfinal", "Quarter")
            .map(FixtureHandlerTest::randomizeString)
            .toList();

    // when
    performTournamentTest(testStrings, Fixture.QuarterFinal);
  }

  @Test
  @DisplayName("Validate creation of Semi-final Fixtures")
  void testSemiFinal() {
    // given
    List<String> testStrings =
        Stream.of("Semi Final", "SemiFinal", "Semi-final", "SEMI- finAL", "semi")
            .map(FixtureHandlerTest::randomizeString)
            .toList();

    performTournamentTest(testStrings, Fixture.SemiFinal);
  }

  @Test
  @DisplayName("Validate creation of Final Fixtures")
  void testFinal() {
    // given
    List<String> testStrings =
        Stream.of("Final", "Final      ", "     final     ", " final               ")
            .map(FixtureHandlerTest::randomizeString)
            .toList();

    performTournamentTest(testStrings, Fixture.Final);
  }

  @Test
  @DisplayName("Validate creation of Playoff Fixtures")
  void testPlayoff() {
    // given
    List<String> testStrings =
        Stream.of(
                "Playoff",
                "play-off",
                "play-of",
                "Playof",
                "     play-off   ",
                " playof ",
                "3rd Place Playoff")
            .map(FixtureHandlerTest::randomizeString)
            .toList();

    performTournamentTest(testStrings, Fixture.Playoff);
  }

  private void performTournamentTest(
      @NotNull Iterable<? extends String> testStrings, @NotNull Fixture expected) {
    for (String test : testStrings) {
      logger.info("Testing Fixture generation with: {} ...", test);
      Fixture fixture = createRoundRobinFixture(test);
      logger.info("Created Fixture: {}", fixture);

      // then
      assertThat(fixture).isEqualTo(expected);
    }
  }
}
