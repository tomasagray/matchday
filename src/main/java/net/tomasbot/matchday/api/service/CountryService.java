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

package net.tomasbot.matchday.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.tomasbot.matchday.db.CountryRepository;
import net.tomasbot.matchday.model.Country;
import net.tomasbot.matchday.util.ResourceFileReader;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CountryService {

  private final CountryRepository countryRepository;

  public CountryService(CountryRepository countryRepository) {
    this.countryRepository = countryRepository;
  }

  public List<Country> getAllCountries() {
    final List<Country> all = countryRepository.findAll();
    all.forEach(country -> Hibernate.initialize(country.getLocales()));
    return all;
  }

  public Optional<Country> getCountry(@NotNull String name) {
    final Optional<Country> optional = countryRepository.findById(name);
    optional.ifPresent(country -> Hibernate.initialize(country.getLocales()));
    return optional;
  }

  public List<Country> saveAll(Collection<Country> countries) {
    return countryRepository.saveAll(countries);
  }

  /**
   * Get the flag image for the specified country. Empty if no data is read.
   *
   * @param countryName The (primary) name of the Country
   * @return a byte array of the image, or empty()
   * @throws IllegalArgumentException if a Country cannot be found for the specified name
   */
  public byte[] getFlag(@NotNull String countryName) throws IOException {
    final Optional<Country> optional = countryRepository.findById(countryName);
    if (optional.isPresent()) {
      final Country country = optional.get();
      return readFlagData(country);
    }
    throw new IllegalArgumentException("No such country: " + countryName);
  }

  private byte @NotNull [] readFlagData(@NotNull Country country) throws IOException {
    final String path = country.getFlagFileName();
    final String data = ResourceFileReader.readTextResource(path);
    return data.getBytes(StandardCharsets.UTF_8);
  }
}
