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

package self.me.matchday.unit.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.util.ResourceFileReader;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceFileReaderTest {

  private static final Logger logger = LogManager.getLogger(ResourceFileReaderTest.class);
  private static final String PROPERTIES_FILE = "plugins/disk-manager/disk-manager.properties";

  @Test
  @DisplayName("Ensure reads and splits resource file key/value pairs correctly")
  void testReadPropertiesResourceFile() throws IOException {

    Map<String, String> resourceFile = ResourceFileReader.readPropertiesResource(PROPERTIES_FILE);

    // Perform tests
    assertThat(resourceFile.size()).isGreaterThan(0);

    resourceFile.forEach(
        (key, value) -> {
          logger.info(String.format("Read key: [%s], value: %s", key, value));
          assertThat(key).isNotNull().isNotEmpty();
          // not comments
          if (!key.startsWith("#")) {
            assertThat(value).isNotNull();
          }
        });
  }
}
