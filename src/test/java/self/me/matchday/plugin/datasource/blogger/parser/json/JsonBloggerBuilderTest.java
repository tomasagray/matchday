/*
 * Copyright (c) 2021.
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

package self.me.matchday.plugin.datasource.blogger.parser.json;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.CreateTestData;
import self.me.matchday.plugin.datasource.blogger.Blogger;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonBloggerBuilderTest {

  private static final String LOG_TAG = "JsonBloggerBuilderTest";

  private static Blogger blogger;

  @BeforeAll
  static void setUp() throws IOException {

    // Create parser
    JsonBloggerBuilder bloggerBuilder =
        new JsonBloggerBuilder(CreateTestData.ZKF_JSON_URL, new JsonPostBuilderFactory());
    // Get Blogger from remote source
    blogger = bloggerBuilder.getBlogger();
  }

  @Test
  @DisplayName("Verify blog ID")
  void testBloggerId() {

    final String actualBlogId = blogger.getBlogId();
    final String expectedBlogId = "3404769062477783101";

    Log.i(LOG_TAG, "Testing blog ID: " + actualBlogId);
    assertThat(actualBlogId).isEqualTo(expectedBlogId);
  }

  @Test
  @DisplayName("Verify blog title parsing")
  void testTitle() {

    final String actualTitle = blogger.getTitle();
    final String expectedTitle = "zkfootballmatches";

    Log.i(LOG_TAG, "Testing blog title: " + actualTitle);
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate Blogger link parsing")
  void testLink() throws MalformedURLException {

    final String actualLink = blogger.getLink();
    final String expectedLink = "https://zkfootballmatch.blogspot.com/";

    Log.i(LOG_TAG, "Testing Blogger link: " + actualLink);
    // Validate URL
    final URL actualUrl = new URL(actualLink);
    final URL expectedUrl = new URL(expectedLink);

    assertThat(actualUrl).isEqualTo(expectedUrl);
  }

  @Test
  @DisplayName("Validate Blogger author")
  void testAuthor() {

    final String actualAuthor = blogger.getAuthor();
    final String expectedAuthor = "zkfootballmatches";

    Log.i(LOG_TAG, "Testing Blogger author: " + actualAuthor);
    assertThat(actualAuthor).isEqualTo(expectedAuthor);
  }

  @Test
  @DisplayName("Validate Blogger version")
  void testVersion() {

    final String actualVersion = blogger.getVersion();
    final String expectedVersion = "1.0";

    Log.i(LOG_TAG, "Testing Blogger version: " + actualVersion);
    assertThat(actualVersion).isEqualTo(expectedVersion);
  }

  @Test
  @DisplayName("Validate Blogger posts")
  void testPosts() {

    final long actualPostCount = blogger.getPostCount();
    final long expectedPostCount = 25;
    final Stream<BloggerPost> actualPosts = blogger.getPosts();

    Log.i(LOG_TAG, String.format("Testing Blogger posts, found %s posts", actualPostCount));
    assertThat(actualPostCount).isEqualTo(expectedPostCount);
    actualPosts.forEach(
        bloggerPost -> {
          // Test each post
          final String postTitle = bloggerPost.getTitle();
          Log.i(LOG_TAG, "Testing post title: " + postTitle);
          assertThat(postTitle).isNotEmpty();
        });
  }
}
