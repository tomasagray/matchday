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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.db.ProperNameRepository;
import self.me.matchday.db.SynonymRepository;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;

@Service
public class SynonymService implements EntityService<Synonym, Long> {

  private final SynonymRepository synonymRepository;
  private final ProperNameRepository properNameRepository;

  public SynonymService(
      SynonymRepository synonymRepository, ProperNameRepository properNameRepository) {
    this.synonymRepository = synonymRepository;
    this.properNameRepository = properNameRepository;
  }

  public void validateProperName(@NotNull ProperName properName) {

    final Set<Synonym> synonyms = properName.getSynonyms();
    // get ids
    final List<Long> synonymIds = getSynonymIds(synonyms);
    synonyms.forEach(
        synonym -> {
          validateSynonym(synonym, synonymIds);
          validateSynonymIsNotProperName(synonym);
        });
  }

  public void validateSynonym(Synonym synonym, List<Long> synonymIds) {

    if (synonym == null) {
      throw new IllegalArgumentException("Synonym was null");
    }
    final String name = synonym.getName();
    if (name == null || "".equals(name)) {
      throw new IllegalArgumentException("Found empty Synonym");
    }
    // check if synonym already exists
    final Optional<Synonym> synonymOptional = findSynonym(name);
    if (synonymOptional.isPresent()) {
      final Synonym existingSynonym = synonymOptional.get();
      // synonym exists, but associated with another entity?
      if (!synonymIds.contains(existingSynonym.getId())) {
        final String msg =
            String.format("Synonym: %s already exists and is associated with another entity", name);
        throw new IllegalArgumentException(msg);
      }
    }
  }

  private Optional<Synonym> findSynonym(String name) {
    List<Synonym> synonyms = synonymRepository.findSynonymByName(name);
    if (synonyms.size() > 1) {
      String msg = String.format("Duplicated Synonyms found for %s: %s", name, synonyms);
      throw new IllegalArgumentException(msg);
    } else if (synonyms.size() == 1) {
      return Optional.of(synonyms.get(0));
    } else {
      return Optional.empty();
    }
  }

  public void validateSynonymIsNotProperName(@NotNull Synonym synonym) {

    final String name = synonym.getName();
    final List<ProperName> matchingProperNames = properNameRepository.findProperNameByName(name);
    if (matchingProperNames.size() > 0) {
      final String msg =
          String.format("Synonym [%s] already exists as ProperName: %s", name, matchingProperNames);
      throw new IllegalArgumentException(msg);
    }
  }

  @NotNull
  private List<Long> getSynonymIds(@NotNull Set<Synonym> synonyms) {
    return synonyms.stream().map(Synonym::getId).collect(Collectors.toList());
  }

  @Override
  public Synonym initialize(@NotNull Synonym synonym) {
    Hibernate.initialize(synonym);
    return synonym;
  }

  @Override
  public Synonym save(@NotNull Synonym synonym) {
    return synonymRepository.save(synonym);
  }

  @Override
  public List<Synonym> saveAll(@NotNull Iterable<? extends Synonym> synonyms) {
    return StreamSupport.stream(synonyms.spliterator(), false)
        .map(this::save)
        .collect(Collectors.toList());
  }

  public ProperName addProperName(@NotNull ProperName properName) {
    return properNameRepository.save(properName);
  }

  @Override
  public Optional<Synonym> fetchById(@NotNull Long id) {
    return synonymRepository.findById(id);
  }

  @Override
  public List<Synonym> fetchAll() {
    return synonymRepository.findAll();
  }

  public Optional<Synonym> fetchByName(@NotNull String name) {
    return findSynonym(name);
  }

  public ProperName fetchProperNameFor(@NotNull String synonym) {
    return properNameRepository
        .findProperNameForSynonym(synonym)
        .orElseThrow(
            () -> new IllegalArgumentException("No proper name found for synonym: " + synonym));
  }

  /**
   * Returns all words (Synonyms and ProperNames) matching the given word.
   *
   * @param word The string (word) to search for
   * @return A list of matching synonyms for the given word
   */
  public List<String> fetchSynonymsFor(@NotNull String word) {
    final Stream<String> synonyms =
        synonymRepository.findSynonymsFor(word).stream().map(Synonym::getName);
    final Stream<String> properNames =
        properNameRepository.findProperNameByName(word).stream().map(ProperName::getName);
    return Stream.concat(synonyms, properNames).collect(Collectors.toList());
  }

  @Override
  public Synonym update(@NotNull Synonym synonym) {
    if (synonym.getId() == null) {
      throw new IllegalArgumentException("Trying to update unknown Synonym: " + synonym);
    }
    return this.save(synonym);
  }

  @Override
  public List<Synonym> updateAll(@NotNull Iterable<? extends Synonym> synonyms) {
    return StreamSupport.stream(synonyms.spliterator(), false)
        .map(this::update)
        .collect(Collectors.toList());
  }

  @Override
  public void delete(@NotNull Long id) {
    synonymRepository.deleteById(id);
  }

  @Override
  public void deleteAll(@NotNull Iterable<? extends Synonym> synonyms) {
    synonymRepository.deleteAll(synonyms);
  }
}
