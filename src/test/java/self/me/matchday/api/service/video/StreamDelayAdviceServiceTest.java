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

package self.me.matchday.api.service.video;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for video stream wait delay advice service")
class StreamDelayAdviceServiceTest {

  private static final Logger logger = LogManager.getLogger(StreamDelayAdviceServiceTest.class);

  private static StreamDelayAdviceService delayAdviceService;
  private static VideoStreamLocatorPlaylist testLocatorPlaylist;

  @BeforeAll
  public static void setup(
      @Autowired @NotNull StreamDelayAdviceService delayAdviceService,
      @Autowired @NotNull TestDataCreator testDataCreator) {

    StreamDelayAdviceServiceTest.delayAdviceService = delayAdviceService;
    StreamDelayAdviceServiceTest.testLocatorPlaylist =
        testDataCreator.createStreamLocatorPlaylist();
  }

  @Test
  @DisplayName("Validate sane advice is given")
  void getDelayAdvice() {

    final int expectedDelayAdvice = 16_750;
    logger.info("Attempting to get delay advice for locator playlist:\n{}", testLocatorPlaylist);
    final long delayAdvice = delayAdviceService.getDelayAdvice(testLocatorPlaylist);
    logger.info("Got stream wait advice: {}", delayAdvice);
    assertThat(delayAdvice).isNotZero().isEqualTo(expectedDelayAdvice);
  }

  @Test
  @Disabled
  void scheduleFileServerPings() {
    delayAdviceService.pingActiveFileServers();
  }
}
