package self.me.matchday.feed;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import self.me.matchday.TestConstants;
import self.me.matchday.feed.galataman.GalatamanBlog;
import self.me.matchday.feed.galataman.GalatamanPost;
import self.me.matchday.feed.galataman.GalatamanPostProcessor;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

class IMatchSourceTest {
  private static final String LOG_TAG = "IMatchSourceTest";

  private static Blogger currentBlog;
  private static Blogger knownGoodBlog;

  /**
   * Read test data, both a known good source and the latest edition. Both should contain exactly 25
   * entries.
   */
  @BeforeAll
  static void setup() {
    try {
      currentBlog =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_CONTEMPORARY_JSON), new GalatamanPostProcessor());
      knownGoodBlog =
          new GalatamanBlog(
              new URL(TestConstants.REMOTE_KNOWN_GOOD_JSON), new GalatamanPostProcessor());

    } catch (IOException e) {
      Log.d(LOG_TAG, "Could not read test data.");
    }
  }

  /**
   * Provide a Stream of GalatamanPosts
   *
   * @return Stream<Arguments> A stream of Arguments for test methods
   */
  @NotNull
  private static Stream<Arguments> getArguments() {
    return Stream.concat(
        currentBlog.getEntries().stream()
            .map(Arguments::of),
        knownGoodBlog.getEntries().stream()
            .map(Arguments::of));
  }

  @DisplayName("Ensure each Post can be parsed into a match")
  @ParameterizedTest(name = "Testing: {index}; {0}")
  @MethodSource("getArguments")
  void getMatch(@NotNull IMatchSource matchSource) {
    // Ensure match can be parsed
    System.out.println("Testing post: " + matchSource);
    assertNotNull(matchSource.getMatch());
  }


  @DisplayName("Ensure parses Match metadata")
  @ParameterizedTest(name = "Testing: {index}; {0}")
  @MethodSource("getArguments")
  void ensureParsesMatchData(@NotNull GalatamanPost galatamanPost) {
    Match match = null;

    try {
      match = galatamanPost.getMatch();
      System.out.println("Post URL: " + galatamanPost.getLink());
      System.out.println("Testing: \n" + match);

      // Tests
      if(match.getHomeTeam() != null) {
        assertNotNull( match.getAwayTeam() );
      }
      assertNotNull(match.getCompetition());
      assertNotNull(match.getSeason());
      assertNotEquals(0, match.getSeason().getStartDate().getYear());
      assertNotNull(match.getDate());
      assertNotNull(match.getFixture());

      System.out.println("All tests passed.");
    } catch (AssertionError e) {
      Log.e(LOG_TAG, "Test FAILED for Match: \n" + match + "\n @ URL: " + galatamanPost.getLink()
          + "\n\tMessage: " + e.getMessage() + "\n\n", e);
      throw e;
    } catch (NullPointerException e) {
      Log.e(LOG_TAG, "Null pointer exception when testing post:\n" + galatamanPost, e);
      throw e;
    }
  }
}