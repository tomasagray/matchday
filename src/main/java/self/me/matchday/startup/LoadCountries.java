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

package self.me.matchday.startup;

import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.CountryService;
import self.me.matchday.model.Country;
import self.me.matchday.util.JsonParser;

@Component
public class LoadCountries implements CommandLineRunner {

  @Value("${artwork.flag-uri-format}")
  private static String FLAG_URI;

  private static final String COUNTRIES_JSON = "countries.json";
  private static final int EXPECTED_COUNTRIES = 245;

  private final Logger logger = LogManager.getLogger(LoadCountries.class);
  private final CountryService countryService;

  public LoadCountries(CountryService countryService) {
    this.countryService = countryService;
  }

  private List<Country> readCountriesJson() throws IOException {
    final URL resource = getClass().getClassLoader().getResource(COUNTRIES_JSON);
    if (resource == null) {
      throw new FileNotFoundException("Could not read resource file: countries.json");
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(resource.getFile()))) {
      final Type type = new TypeToken<List<Country>>() {}.getType();
      return JsonParser.fromJson(reader, type);
    }
  }

  private boolean isCountriesProperlyLoaded() {
    final List<Country> allCountries = countryService.getAllCountries();
    final int countryCount = allCountries.size();
    if (countryCount > 0 && countryCount != EXPECTED_COUNTRIES) {
      final String msg =
          String.format(
              "Found partial Country (%d countries) data; database has possibly been corrupted. "
                  + "Perform a manual repair, then reload application.",
              countryCount);
      throw new IllegalArgumentException(msg);
    }

    return countryCount == EXPECTED_COUNTRIES
        && allCountries.stream()
            .map(
                country ->
                    country.getLocales().size() > 0
                        && country.getFlagFileName() != null
                        && !"".equals(country.getName()))
            .reduce((b1, b2) -> b1 && b2)
            .orElse(false);
  }

  @Override
  public void run(String... args) throws Exception {

    logger.info("Validating country data has been loaded...");
    final boolean loaded = isCountriesProperlyLoaded();
    if (!loaded) {
      logger.info("No country data found; loading...");
      final List<Country> countries = readCountriesJson();
      final List<Country> saved = countryService.saveAll(countries);
      logger.info("Saved: {} countries...", saved.size());
    } else {
      logger.info("Country data already loaded, nothing to do");
    }
  }
}
