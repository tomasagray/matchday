/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

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
import self.me.matchday.feed.blogger.galataman.GalatamanBlog;
import self.me.matchday.feed.blogger.galataman.GalatamanPost;
import self.me.matchday.feed.blogger.galataman.GalatamanPostProcessor;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventSource;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Match;
import self.me.matchday.util.Log;

class IEventSourceTest {
  private static final String LOG_TAG = "IEventSourceTest";

  private static IEventRepository currentBlog;
  private static IEventRepository knownGoodBlog;

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
      Log.d(LOG_TAG, "Could not read test data.", e);
    }
  }

  /**
   * Provide a Stream of GalatamanPosts
   *
   * @return Stream<Arguments> A stream of Arguments for test methods
   */
  @NotNull
  private static Stream<Arguments> getAllEvents() {
    return Stream.concat(
        currentBlog.getEventSources().map(Arguments::of), knownGoodBlog.getEventSources().map(Arguments::of));
  }

  @NotNull
  private static Stream<Arguments> getMatches() {
    return Stream.concat(
        currentBlog.getEventSources().filter(e -> e.getEvent() instanceof Match).map(Arguments::of),
        knownGoodBlog.getEventSources().filter(e -> e.getEvent() instanceof Match).map(Arguments::of));
  }

  @NotNull
  private static Stream<Arguments> getHighlightShows() {
    return Stream.concat(
        currentBlog
            .getEventSources()
            .filter(e -> e.getEvent() instanceof HighlightShow)
            .map(Arguments::of),
        knownGoodBlog
            .getEventSources()
            .filter(e -> e.getEvent() instanceof HighlightShow)
            .map(Arguments::of));
  }

  @DisplayName("Ensure each Post can be parsed into a Event")
  @ParameterizedTest(name = "Testing: {index}; {0}")
  @MethodSource("getAllEvents")
  void ensureParsesEvents(@NotNull EventSource eventSource) {
    // Cast to GalatamanPost for identification purposes
    GalatamanPost gp = (GalatamanPost) eventSource;
    System.out.println("Testing post: [" + gp.getBloggerPostID() + "] @ " + gp.getLink() + "\n\n");

    // Ensure Event can be parsed
    assertNotNull(eventSource.getEvent());
    System.out.println("Successfully parsed event:\n" + eventSource.getEvent());
  }

  @DisplayName("Ensure parses Match metadata")
  @ParameterizedTest(name = "Testing: {index}; {0}")
  @MethodSource("getMatches")
  void ensureParsesMatchData(@NotNull EventSource eventSource) {
    Event event = null;
    // Get the link
    final String postLink = eventSource.getLink();

    try {
      event = eventSource.getEvent();
      // Ensure we are dealing with a Match; Highlights are another test!
      if (!(event instanceof Match)) {
        System.out.println("Not a match; skipping...");
        throw new AssertionError("Event was not a Match!");
      }

      Match match = (Match) event;
      System.out.println("Post URL: " + postLink);
      System.out.println("Testing: \n" + match);

      // Tests
      if (match.getHomeTeam() != null) {
        assertNotNull(match.getAwayTeam());
      }
      assertNotNull(match.getCompetition());
      assertNotNull(match.getSeason());
      assertNotEquals(0, match.getSeason().getStartDate().getYear());
      assertNotNull(match.getDate());
      assertNotNull(match.getFixture());

      System.out.println("All tests passed.");
    } catch (AssertionError e) {
      Log.e(
          LOG_TAG,
          "Test FAILED for Event: \n"
              + event
              + "\n @ URL: "
              + postLink
              + "\n\tMessage: "
              + e.getMessage()
              + "\n\n",
          e);
      throw e;
    } catch (NullPointerException e) {
      Log.e(LOG_TAG, "Null pointer exception when testing post:\n" + eventSource, e);
      throw e;
    }
  }

  @DisplayName("Ensure parses Highlight Show metadata correctly")
  @ParameterizedTest(name = "Testing: {index}")
  @MethodSource("getHighlightShows")
  void ensureParsesHighlightData(@NotNull EventSource eventSource) {
    Event event = null;
    // Get the link
    final String postLink = eventSource.getLink();

    try {
      event = eventSource.getEvent();
      // Ensure we are dealing with a Highlight Show
      if (!(event instanceof HighlightShow)) {
        System.out.println("Not a HighlightShow; skipping...");
        throw new AssertionError("Event was not a Highlight!");
      }

      final HighlightShow highlightShow = (HighlightShow) event;
      System.out.println("Post URL: " + postLink);
      System.out.println("Testing: \n" + highlightShow);

      // Tests
      assertNotNull(highlightShow.getTitle());
      assertNotEquals("", highlightShow.getTitle());
      assertNotNull(highlightShow.getCompetition());
      assertNotNull(highlightShow.getSeason());
      assertNotNull(highlightShow.getFixture());
      assertNotNull(highlightShow.getDate());

      System.out.println("All tests passed.");
    } catch (AssertionError e) {
      Log.e(
          LOG_TAG,
          "Test FAILED for Event: \n"
              + event
              + "\n @ URL: "
              + postLink
              + "\n\tMessage: "
              + e.getMessage()
              + "\n\n",
          e);
      throw e;
    } catch (NullPointerException e) {
      Log.e(LOG_TAG, "Null pointer exception when testing post:\n" + eventSource, e);
      throw e;
    }
  }
}
