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

package self.me.matchday.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Country;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation for Country API")
class CountryServiceTest {

  private static final Logger logger = LogManager.getLogger(CountryServiceTest.class);
  private static CountryService countryService;

  @BeforeAll
  static void setup(@Autowired CountryService countryService) {
    CountryServiceTest.countryService = countryService;
  }

  @Test
  @DisplayName("Ensure expected country count is achieved")
  void getAllCountries() {

    // given
    final int expectedCountry = 245;
    logger.info("Expected country count: {}", expectedCountry);

    // when
    final List<Country> allCountries = countryService.getAllCountries();
    final int actualCountryCount = allCountries.size();
    logger.info("Found: {} countries", actualCountryCount);
    // then
    assertThat(actualCountryCount).isEqualTo(expectedCountry);
  }

  @Test
  @DisplayName("Validate retrieving a particular country by name")
  void getCountry() {

    // given
    final String countryName = "Spain";
    logger.info("Looking up: {}", countryName);

    // when
    final Optional<Country> countryOptional = countryService.getCountry(countryName);
    // then
    assertThat(countryOptional).isPresent();
    final Country country = countryOptional.get();
    logger.info("Found: {}", country);
    assertThat(country.getName()).isEqualTo(countryName);
  }

  @Test
  @DisplayName("Validate reading flag image data for a specified country")
  void getFlag() throws IOException {

    // given
    final long expectedBytes = 91_814;
    final String countryName = "Spain";
    logger.info("Reading flag for Country: {}; expecting {} bytes", countryName, expectedBytes);

    // when
    final byte[] flagData = countryService.getFlag(countryName);
    final int actualBytes = flagData.length;
    logger.info("Read: {} bytes", actualBytes);
    logger.info("Read data:\n{}", new String(flagData));
    // then
    assertThat(actualBytes).isEqualTo(expectedBytes);
  }
}
