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
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.PatternKitTemplateRepository;
import self.me.matchday.model.PatternKitTemplate;

@Service
@Transactional
public class PatternKitTemplateService {

  private final PatternKitTemplateRepository templateRepository;

  PatternKitTemplateService(PatternKitTemplateRepository templateRepository) {
    this.templateRepository = templateRepository;
  }

  public List<PatternKitTemplate> fetchAll() {
    return templateRepository.findAll();
  }

  public Optional<PatternKitTemplate> fetchById(@NotNull Long id) {
    return templateRepository.findById(id);
  }

  public Optional<PatternKitTemplate> fetchByClassName(@NotNull String className) {
    return templateRepository.findPatternKitTemplateByNameEquals(className);
  }

  public PatternKitTemplate save(@NotNull PatternKitTemplate entity) {
    return templateRepository.save(entity);
  }

  public PatternKitTemplate update(@NotNull PatternKitTemplate entity) {
    final Optional<PatternKitTemplate> templateOptional =
        templateRepository.findById(entity.getId());
    if (templateOptional.isPresent()) {
      save(entity);
    }
    throw new IllegalArgumentException(
        "Trying to update non-existent PatternKitTemplate: " + entity);
  }

  public void delete(@NotNull PatternKitTemplate entity) {
    templateRepository.delete(entity);
  }
}
