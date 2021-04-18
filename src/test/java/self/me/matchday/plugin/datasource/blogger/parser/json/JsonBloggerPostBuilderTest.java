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

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.io.JsonStreamReader;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.util.Log;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonBloggerPostBuilderTest {

  private static final String LOG_TAG = "JsonBloggerPostBuilderTest";

  private static BloggerPost bloggerPost;

  @BeforeAll
  static void setUp() throws IOException {

    // Read test data file
    final String postData =
        String.join(
            "",
            ResourceFileReader.readTextResource(JsonBloggerPostBuilderTest.class, "zkf_post.json"));
    final JsonObject jsonObject = JsonStreamReader.readJsonString(postData);
    // Get JSON object for post
    final JsonBloggerPostBuilder postBuilder =
        new JsonBloggerPostBuilder(jsonObject.get("entry").getAsJsonObject());
    bloggerPost = postBuilder.getBloggerPost();
  }

  @Test
  @DisplayName("Validate Blogger post ID")
  void testPostId() {

    final String actualPostId = bloggerPost.getBloggerPostID();
    final String expectedPostId = "830287824721606564";

    Log.i(LOG_TAG, "Testing blogger post ID: " + actualPostId);
    assertThat(actualPostId).isEqualTo(expectedPostId);
  }

  @Test
  @DisplayName("Verify parses title")
  void testTitle() {

    final String actualTitle = bloggerPost.getTitle();
    final String expectedTitle = "International Friendly - Austria vs Greece - 07/10/2020";

    Log.i(LOG_TAG, "Testing post title: " + actualTitle);
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate post publish date")
  void testPublishDate() {

    final LocalDateTime actualPublishDate = bloggerPost.getPublished();
    final LocalDateTime expectedPublishDate = LocalDateTime.parse("2020-10-07T21:49");

    Log.i(
        LOG_TAG,
        "Testing post publish date: "
            + actualPublishDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    assertThat(actualPublishDate).isEqualTo(expectedPublishDate);
  }

  @Test
  @DisplayName("Validate post update date")
  void testUpdated() {

    final LocalDateTime actualLastUpdated = bloggerPost.getLastUpdated();
    final LocalDateTime expectedLastUpdated = LocalDateTime.parse("2020-10-07T21:49:17");

    Log.i(LOG_TAG, "Testing post last updated date: " + actualLastUpdated);
    assertThat(actualLastUpdated).isEqualTo(expectedLastUpdated);
  }

  @Test
  @DisplayName("Validate post content")
  void testContent() {

    final String actualContent = bloggerPost.getContent();
    final String expectedContent =
        "<p>&nbsp;<a href=\"https://imgbox.com/nYScMV6i\" style=\"background-color: #fafafa; color: #920504; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; "
            + "text-align: center;\" target=\"_blank\"><img alt=\"\" border=\"0\" "
            + "src=\"https://images2.imgbox.com/c0/61/nYScMV6i_o.jpg\" style=\"border: 0px; max-width: 100%;\" />"
            + "</a></p><br style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, "
            + "Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\" /><b "
            + "style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, Calibri,"
            + " Geneva, sans-serif; font-size: 13px; text-align: center;\"><span style=\"font-size: large;\">"
            + "International Friendly - 07/10/2020</span></b><br style=\"background-color: #fafafa; color: "
            + "#333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; "
            + "text-align: center;\" /><span style=\"background-color: #fafafa; color: #333333; font-family: "
            + "Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: x-large; text-align: center;\">"
            + "<b>Austria vs Greece</b></span><br style=\"background-color: #fafafa; color: #333333; font-family: "
            + "Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\" />"
            + "<span><a name='more'></a></span><span style=\"background-color: #fafafa; color: #333333; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: small; text-align: "
            + "center;\"><br /><br /><img alt=\"\" border=\"0\" src=\"http://i.imgur.com/zSRcd6q.png\" "
            + "style=\"border: 0px; max-width: 100%;\" /><br /><br /><b>1st half</b><br />"
            + "<a href=\"https://www.inclouddrive.com/file/jbzO0YWphOf54wDPkav5CA/20201007-austria-greece-1-"
            + "ger-720p50.mkv\" style=\"color: black; text-decoration-line: none;\" target=\"_blank\">"
            + "https://www.inclouddrive.com/file/jb...ger-720p50.mkv</a><br /><br /><b>2nd half</b><br /><br />"
            + "<br /><span style=\"font-family: &quot;Franklin Gothic Medium&quot;;\"><b><br />"
            + "<span style=\"color: green;\">channel:</span>&nbsp;ORF Eins HD<br />"
            + "<span style=\"color: green;\">language:</span>&nbsp;<span style=\"color: red;\"><b>german</b>"
            + "</span><br /><span style=\"color: green;\">format:</span>&nbsp;720p 50FPS mkv<br />"
            + "<span style=\"color: green;\">bitrate:</span>&nbsp;4 MB/sec<br /><span style=\"color: green;\">"
            + "size:</span>&nbsp;3,5 GB<br /></b></span></span><br style=\"background-color: #fafafa; "
            + "color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: "
            + "13px; text-align: center;\" /><br style=\"background-color: #fafafa; color: #333333; font-family: "
            + "Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\" />"
            + "<a href=\"https://imgbox.com/zs3IV2yv\" style=\"background-color: #fafafa; color: black; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: "
            + "center; text-decoration-line: none;\" target=\"_blank\"><img alt=\"\" border=\"0\" "
            + "src=\"https://images2.imgbox.com/98/76/zs3IV2yv_o.jpg\" style=\"border: 0px; max-width: 100%;\" "
            + "/></a><span style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, "
            + "Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\"></span><br "
            + "style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, "
            + "Geneva, sans-serif; font-size: 13px; text-align: center;\" /><a href=\"https://imgbox.com/7wjj6Znd\" "
            + "style=\"background-color: #fafafa; color: black; font-family: Verdana, Arial, Tahoma, Calibri, "
            + "Geneva, sans-serif; font-size: 13px; text-align: center; text-decoration-line: none;\" "
            + "target=\"_blank\"><img alt=\"\" border=\"0\" src=\"https://images2.imgbox.com/64/35/7wjj6Znd_o.jpg\" "
            + "style=\"border: 0px; max-width: 100%;\" /></a><br style=\"background-color: #fafafa; color: "
            + "#333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; "
            + "text-align: center;\" /><br style=\"background-color: #fafafa; color: #333333; font-family: "
            + "Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\" />"
            + "<br style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, "
            + "Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\" /><span style=\"background-color: "
            + "#fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; "
            + "font-size: 13px; text-align: center;\">________________________</span><br style=\"background-color: "
            + "#fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; "
            + "font-size: 13px; text-align: center;\" /><br style=\"background-color: #fafafa; color: #333333; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: "
            + "center;\" /><br style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, "
            + "Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\" /><br "
            + "style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, "
            + "Geneva, sans-serif; font-size: 13px; text-align: center;\" /><br style=\"background-color: #fafafa; "
            + "color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; "
            + "text-align: center;\" /><b style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, "
            + "Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\"><span style=\"font-size: "
            + "large;\">International Friendly - 07/10/2020</span></b><br style=\"background-color: #fafafa; "
            + "color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: "
            + "13px; text-align: center;\" /><span style=\"background-color: #fafafa; color: #333333; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: x-large; "
            + "text-align: center;\"><b>Austria vs Greece</b></span><br style=\"background-color: #fafafa; "
            + "color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: "
            + "13px; text-align: center;\" /><span style=\"background-color: #fafafa; color: #333333; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: small; text-align: "
            + "center;\"><br /><br /><img alt=\"\" border=\"0\" src=\"http://i.imgur.com/zSRcd6q.png\" "
            + "style=\"border: 0px; max-width: 100%;\" /><br /><br /><b>1st half</b><br /><a "
            + "href=\"https://www.inclouddrive.com/file/fgdEOxJcMU20hb1CoJnP9A/20201007-Austria-vs-Greece-1-eng"
            + "-720p60.mkv\" style=\"color: black; text-decoration-line: none;\" target=\"_blank\">"
            + "https://www.inclouddrive.com/file/fg...eng-720p60.mkv</a><br /><br /><b>2nd half</b><br /><br />"
            + "<br /><span style=\"font-family: &quot;Franklin Gothic Medium&quot;;\"><b><br /><span "
            + "style=\"color: green;\">channel:</span>&nbsp;ESPN<br /><span style=\"color: green;\">language:"
            + "</span>&nbsp;<span style=\"color: red;\"><b>english</b></span><br /><span style=\"color: green;\">"
            + "format:</span>&nbsp;720p 60FPS mkv<br /><span style=\"color: green;\">bitrate:</span>&nbsp;7 "
            + "MB/sec<br /><span style=\"color: green;\">size:</span>&nbsp;6 GB<br /></b></span></span><br "
            + "style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, "
            + "Geneva, sans-serif; font-size: 13px; text-align: center;\" /><br style=\"background-color: "
            + "#fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; "
            + "font-size: 13px; text-align: center;\" /><a href=\"https://imgbox.com/8jUMy2mg\" "
            + "style=\"background-color: #fafafa; color: black; font-family: Verdana, Arial, Tahoma, Calibri, "
            + "Geneva, sans-serif; font-size: 13px; text-align: center; text-decoration-line: none;\" "
            + "target=\"_blank\"><img alt=\"\" border=\"0\" src=\"https://images2.imgbox.com/70/1d/8jUMy2mg_o.jpg\" "
            + "style=\"border: 0px; max-width: 100%;\" /></a><span style=\"background-color: #fafafa; color: #333333; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: "
            + "center;\">&nbsp;</span><a href=\"https://imgbox.com/9G7EwoD1\" style=\"background-color: #fafafa; "
            + "color: black; font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; "
            + "text-align: center; text-decoration-line: none;\" target=\"_blank\"><img alt=\"\" border=\"0\" "
            + "src=\"https://images2.imgbox.com/b3/24/9G7EwoD1_o.jpg\" style=\"border: 0px; max-width: 100%;\" "
            + "/></a><span style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, "
            + "Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\">&nbsp;</span><a "
            + "href=\"https://imgbox.com/tDjYF96M\" style=\"background-color: #fafafa; color: black; "
            + "font-family: Verdana, Arial, Tahoma, Calibri, Geneva, sans-serif; font-size: 13px; text-align: "
            + "center; text-decoration-line: none;\" target=\"_blank\"><img alt=\"\" border=\"0\" src=\""
            + "https://images2.imgbox.com/b4/4a/tDjYF96M_o.jpg\" style=\"border: 0px; max-width: 100%;\" /></a>"
            + "<br style=\"background-color: #fafafa; color: #333333; font-family: Verdana, Arial, Tahoma, "
            + "Calibri, Geneva, sans-serif; font-size: 13px; text-align: center;\" />";

    Log.i(LOG_TAG, "Testing post content\n=========================\n" + actualContent);
    assertThat(actualContent).isEqualTo(expectedContent);
  }

  @Test
  @DisplayName("Validate post link")
  void testLink() throws MalformedURLException {

    final String actualLink = bloggerPost.getLink();
    final String expectedLink =
            "https://zkfullmatchvideos.blogspot.com/2020/10/international-friendly-austria-vs.html";

    Log.i(LOG_TAG, "Testing post link: " + actualLink);
    // URL-ify - first stage of validation
    final URL actualUrl = new URL(actualLink);
    final URL expectedUrl = new URL(expectedLink);
    assertThat(actualUrl).isEqualTo(expectedUrl);
  }

  @Test
  @DisplayName("Validate post categories")
  void testCategories() {

    final List<String> actualCategories = bloggerPost.getCategories();
    final List<String> expectedCategories =
        List.of("Austria National Team", "Friendly matches", "Greece National Team");

    Log.i(LOG_TAG, "Testing post categories: " + actualCategories);
    assertThat(actualCategories).isEqualTo(expectedCategories);
  }
}
