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
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.db.ProperNameRepository;
import self.me.matchday.db.SynonymRepository;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;

@Service
public class SynonymService {

  private final SynonymRepository synonymRepository;
  private final ProperNameRepository properNameRepository;

  public SynonymService(
      SynonymRepository synonymRepository, ProperNameRepository properNameRepository) {
    this.synonymRepository = synonymRepository;
    this.properNameRepository = properNameRepository;
  }

  public void validateProperName(@NotNull ProperName properName) {

    final List<Synonym> synonyms = properName.getSynonyms();
    // get ids
    final List<Long> synonymIds = getSynonymIds(synonyms);
    synonyms.forEach(
        synonym -> {
          validateSynonym(synonym, synonymIds);
          validateSynonymIsNotProperName(synonym);
        });
  }

  public void validateSynonym(@NotNull Synonym synonym, List<Long> synonymIds) {

    final String name = synonym.getName();
    if (name == null || "".equals(name)) {
      throw new IllegalArgumentException("Found empty Synonym");
    }
    // check if synonym already exists
    final Optional<Synonym> synonymOpt = synonymRepository.findSynonymByName(name);
    if (synonymOpt.isPresent()) {
      final Synonym existingSynonym = synonymOpt.get();
      // synonym exists, but associated with another entity?
      if (!synonymIds.contains(existingSynonym.getId())) {
        final String msg =
            String.format("Synonym: %s already exists and is associated with another entity", name);
        throw new IllegalArgumentException(msg);
      }
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
  private List<Long> getSynonymIds(@NotNull List<Synonym> synonyms) {
    return synonyms.stream().map(Synonym::getId).collect(Collectors.toList());
  }
}
