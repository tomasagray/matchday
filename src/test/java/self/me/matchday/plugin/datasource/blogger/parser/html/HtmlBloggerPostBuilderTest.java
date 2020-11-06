/*
 * Copyright (c) 2020. 
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

import org.assertj.core.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.util.ResourceFileReader;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HtmlBloggerPostBuilderTest - Verify HTML Blogger post parser")
class HtmlBloggerPostBuilderTest {

    private static final String LOG_TAG = "HtmlBloggerPostBuilderTest";

    // Test resources
    private static BloggerPost bloggerPost;

    @BeforeAll
    static void setUp() throws IOException {

        // Read test data
        final String html =
                Strings.join(
                        ResourceFileReader.readTextResource(HtmlBloggerPostBuilderTest.class, "gman_post.html"))
                        .with(" ");
        // Parse to DOM
        final Document document = Jsoup.parse(html);
        // Get BloggerPost
        bloggerPost = new HtmlBloggerPostBuilder(document).getBloggerPost();
    }

    @Test
    @DisplayName("Verify valid Blogger post ID")
    void testPostId() {

        final String actualPostId = bloggerPost.getBloggerPostID();
        final Pattern requiredPattern = Pattern.compile("\\d{19}");

        Log.i(LOG_TAG, "Testing Blogger post ID: " + actualPostId);
        assertThat(actualPostId).matches(requiredPattern);
    }

    @Test
    @DisplayName("Verify correct categories")
    void testCategories() {

        final List<String> actualCategories = bloggerPost.getCategories();
        final List<String> expectedCategories = List.of("Bundesliga", "Sportschau");

        Log.i(LOG_TAG, "Testing categories: " + actualCategories);
        assertThat(actualCategories).isEqualTo(expectedCategories);
    }
    @Test
    @DisplayName("Verify content")
    void testContent() {

        // Parse content
        final String content = bloggerPost.getContent();
        final Document document = Jsoup.parse(content);

        final int actualElementCount = document.getAllElements().size();
        final int expectedElementCount = 105;

        Log.i(LOG_TAG, "Testing post content: " + content);
        assertThat(actualElementCount).isEqualTo(expectedElementCount);
    }

    @Test
    @DisplayName("Verify published date")
    void testPublished() {

        final LocalDateTime actualPublished = bloggerPost.getPublished();

        Log.i(LOG_TAG, "Testing post publish date: " + actualPublished);
        assertThat(actualPublished).isNull();
    }

    @Test
    @DisplayName("Verify updated date")
    void testUpdated() {

        final LocalDateTime lastUpdated = bloggerPost.getLastUpdated();

        Log.i(LOG_TAG, "Testing post last updated date: " + lastUpdated);
        assertThat(lastUpdated).isNull();
    }

    @Test
    @DisplayName("Verify post link")
    void testLink() {

        final String actualLink = bloggerPost.getLink();
        final String expectedLink =
                "https://galatamanhdfb.blogspot.com/2020/06/bundesliga-1920-matchday-32-sportschau_13.html";

        Log.i(LOG_TAG, "Testing post link: " + actualLink);
        assertThat(actualLink).isEqualTo(expectedLink);
    }

    @Test
    @DisplayName("Verify post title")
    void testTitle() {

        final String actualTitle = bloggerPost.getTitle();
        final String expectedTitle = "Bundesliga 19/20 - Matchday 32 - Sportschau - 17/06/2020";

        Log.i(LOG_TAG, "Testing post title: " + actualTitle);
        assertThat(actualTitle).isEqualTo(expectedTitle);
    }
}