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

package self.me.matchday.plugin.datasource.blogger.parser.html;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.CreateTestData;
import self.me.matchday.plugin.datasource.blogger.Blogger;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HtmlBloggerBuilderTest - Verify HTML Blogger parser")
class HtmlBloggerBuilderTest {

  private static final String LOG_TAG = "HtmlBloggerBuilderTest";

  private static Blogger blogger;

  @BeforeAll
  static void setUp() throws IOException {

    // Parse blogger
    HtmlBloggerBuilder bloggerBuilder =
        new HtmlBloggerBuilder(CreateTestData.GMAN_HTML, new HtmlPostBuilderFactory());
    blogger = bloggerBuilder.getBlogger();

    Log.i(LOG_TAG, "Read Blogger from HTML:\n" + blogger);
  }

  @Test
  @DisplayName("Verify correctly parses author data")
  void testAuthor() {

    final String actualAuthor = blogger.getAuthor();
    final String expectedAuthor = "null";

    Log.i(LOG_TAG, "Testing Blogger author: " + actualAuthor);
    assertThat(actualAuthor).isEqualTo(expectedAuthor);
  }

  @Test
  @DisplayName("Verify parses title")
  void testTitle() {

    final String actualTitle = blogger.getTitle();
    final String expectedTitle = "GaLaTaMaN HD Football";

    Log.i(LOG_TAG, "Testing Blog title: " + actualTitle);
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Verify parses blog ID")
  void testBlogId() {

    final String actualBlogId = blogger.getBlogId();
    final String expectedBlogId = "514142039576228363";

    Log.i(LOG_TAG, "Testing blog ID: " + actualBlogId);
    assertThat(actualBlogId).isEqualTo(expectedBlogId);
  }

  @Test
  @DisplayName("Verify parses link")
  void testLink() {

    final String actualLink = blogger.getLink();

    Log.i(LOG_TAG, "Testing blog link: " + actualLink);
    assertThat(actualLink).isNull();
  }

  @Test
  @DisplayName("Verify parses correct # of posts")
  void testPostCount() {

    final long actualPostCount = blogger.getPostCount();
    final long expectedPostCount = 5;

    Log.i(LOG_TAG, "Testing blog post count: " + actualPostCount);
    assertThat(actualPostCount).isEqualTo(expectedPostCount);
  }

  @Test
  @DisplayName("Verify blogger posts are parsed correctly")
  void testPosts() {

    final Stream<BloggerPost> actualPosts = blogger.getPosts();
    actualPosts.forEach(
        bloggerPost -> {
          Log.i(LOG_TAG, "Testing Blogger post:\n" + bloggerPost);
          assertThat(bloggerPost.getTitle()).isNotEmpty();
        });
  }

  @Test
  @DisplayName("Verify blogger version")
  void testVersion() {

    final String actualVersion = blogger.getVersion();
    final String expectedVersion = "";

    Log.i(LOG_TAG, "Testing blogger version: " + actualVersion);
    assertThat(actualVersion).isEqualTo(expectedVersion);
  }
}
