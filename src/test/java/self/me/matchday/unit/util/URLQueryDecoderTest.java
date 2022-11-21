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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import self.me.matchday.util.URLQueryDecoder;

class URLQueryDecoderTest {

  private static final Logger logger = LogManager.getLogger(URLQueryDecoderTest.class);

  private static final String TEST_URL =
      "https://www.google.com/search?q=TEST+STRING&xsrf=ALeKk03oTkf6HW11sfIblis7ggLLdAr7Q:1602377267332&source="
          + "limns&tbm=shop&sa=X&ved=2ahUKEwing8K-qKvsAhX9IDKVjWAVoQ_AUoAXoECCEAw";

  @Test
  @DisplayName("Validate URL query parameter parsing")
  void testDecode() {

    final Map<String, List<String>> actualParams = URLQueryDecoder.decode(TEST_URL);

    // Build test data
    final Map<String, List<String>> expectedParams = new HashMap<>();
    expectedParams.put("q", List.of("TEST STRING"));
    expectedParams.put("xsrf", List.of("ALeKk03oTkf6HW11sfIblis7ggLLdAr7Q:1602377267332"));
    expectedParams.put("source", List.of("limns"));
    expectedParams.put("tbm", List.of("shop"));
    expectedParams.put("sa", List.of("X"));
    expectedParams.put("ved", List.of("2ahUKEwing8K-qKvsAhX9IDKVjWAVoQ_AUoAXoECCEAw"));

    logger.info("Testing parsed params: " + actualParams);
    assertThat(actualParams).isEqualTo(expectedParams);
  }
}
