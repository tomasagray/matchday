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

package self.me.matchday.plugin.fileserver.filefox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for FileFox file server manager plugin properties")
class FileFoxPluginPropertiesTest {

  private static final Logger logger = LogManager.getLogger(FileFoxPluginPropertiesTest.class);

  private static FileFoxPluginProperties pluginProperties;

  @BeforeAll
  static void setUp(@Autowired FileFoxPluginProperties pluginProperties) {
    FileFoxPluginPropertiesTest.pluginProperties = pluginProperties;
  }

  @Test
  @DisplayName("Validate plugin ID")
  void getId() {

    final UUID expectedPluginId = UUID.fromString("4e3389aa-3c86-4541-aaa1-9a6603753921");
    final UUID actualPluginId = UUID.fromString(pluginProperties.getId());
    logger.info("Got plugin ID: " + actualPluginId);
    assertThat(actualPluginId).isEqualTo(expectedPluginId);
  }

  @Test
  @DisplayName("Validate plugin title")
  void getTitle() {

    final String expectedPluginTitle = "FileFox";
    final String actualPluginTitle = pluginProperties.getTitle();
    logger.info("Got plugin title: " + actualPluginTitle);
    assertThat(actualPluginTitle).isEqualTo(expectedPluginTitle);
  }

  @Test
  @DisplayName("Validate plugin description")
  void getDescription() {

    final String expectedDescription =
        "Manager for the FileFox online file service. Translates external "
            + "links into internal, downloadable links.";
    final String actualPluginDescription = pluginProperties.getDescription();
    logger.info("Got plugin description: " + actualPluginDescription);
    assertThat(actualPluginDescription).isEqualTo(expectedDescription);
  }

  @Test
  @DisplayName("Validate FileFox browser user agent")
  void getUserAgent() {

    final String expectedUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:85.0) Gecko/20100101 Firefox/85.0";
    final String actualUserAgent = pluginProperties.getUserAgent();
    logger.info("Got user agent: " + actualUserAgent);
    assertThat(actualUserAgent).isEqualTo(expectedUserAgent);
  }

  @Test
  @DisplayName("Validate URL pattern matcher for external links")
  void getLinkUrlPattern() throws MalformedURLException {

    final URL testUrl = new URL("https://filefox.cc/79muv293uj1r/20210121-EIB-ATM-LL_1.ts");
    final Pattern actualLinkUrlPattern = pluginProperties.getLinkUrlPattern();

    logger.info("Using URL matcher: {} for test URL:\n{}", actualLinkUrlPattern, testUrl);
    final Matcher matcher = actualLinkUrlPattern.matcher(testUrl.toString());
    assertThat(matcher.find()).isTrue();
  }

  @Test
  @DisplayName("Validate direct download URL pattern matcher")
  void getDirectDownloadUrlPattern() throws MalformedURLException {

    final URL testUrl =
        new URL(
            "https://s02.filefox.cc/wojxtujqwup7elabbssq26q4xhnjwzzgnrxcnsikekhsnu2h6db7ggkemibmq5l2px3e5rca/20210121-EIB-ATM-LL_1.ts");
    final Pattern actualUrlPattern = pluginProperties.getDirectDownloadUrlPattern();

    logger.info("Using URL pattern matcher: {} for URL:\n{}", actualUrlPattern, testUrl);
    final Matcher matcher = actualUrlPattern.matcher(testUrl.toString());
    assertThat(matcher.find()).isTrue();
  }

  @Test
  @DisplayName("Validate plugin refresh rate")
  void getRefreshHours() {

    final int expectedRefreshRate = 4;
    final int actualRefreshRate = pluginProperties.getRefreshHours();
    logger.info("Got refresh rate: " + actualRefreshRate);
    assertThat(actualRefreshRate).isEqualTo(expectedRefreshRate);
  }
}
