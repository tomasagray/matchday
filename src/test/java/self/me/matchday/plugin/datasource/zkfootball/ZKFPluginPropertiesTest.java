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

package self.me.matchday.plugin.datasource.zkfootball;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.util.Log;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validate ZKF plugin properties loading")
class ZKFPluginPropertiesTest {

  private static final String LOG_TAG = "ZKFPluginPropertiesTest";

  private static ZKFPluginProperties pluginProperties;

  @BeforeAll
  static void setUp(@Autowired final ZKFPluginProperties _pluginProperties) {
    pluginProperties = _pluginProperties;
  }

  @Test
  @DisplayName("Validate plugin ID loading")
  void testId() {

    final String actualPluginId = pluginProperties.getId();
    Log.i(LOG_TAG, "Testing plugin ID: " + actualPluginId);

    assertThat(actualPluginId).isNotNull().isNotEmpty();
    final UUID parsedPluginId = UUID.fromString(actualPluginId);
    assertThat(parsedPluginId).isNotNull();
  }

  @Test
  @DisplayName("Validate plugin title loading")
  void testTitle() {

    final String actualTitle = pluginProperties.getTitle();
    Log.i(LOG_TAG, "Testing plugin title: " + actualTitle);
    assertThat(actualTitle).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("Validate plugin description loading")
  void getDescription() {

    final String actualDescription = pluginProperties.getDescription();
    Log.i(LOG_TAG, "Testing plugin description: " + actualDescription);
    assertThat(actualDescription).isNotNull().isNotEmpty();
  }

  @Test
  @DisplayName("Validate base URL loading")
  void testBaseUrl() {

    final String actualBaseUrl = pluginProperties.getBaseUrl();
    final String expectedBaseUrl = "fb11710500.blogspot.com";

    Log.i(LOG_TAG, "Testing plugin base URL: " + actualBaseUrl);
    assertThat(actualBaseUrl).isNotNull().isNotEmpty().isEqualTo(expectedBaseUrl);
  }
}
