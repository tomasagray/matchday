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

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.Corrected;
import self.me.matchday.CorrectedOrNull;
import self.me.matchday.db.SynonymRepository;
import self.me.matchday.model.Competition;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;
import self.me.matchday.model.Team;
import self.me.matchday.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EntityCorrectionService {

  private final SynonymRepository synonymRepository;
  private final CompetitionService competitionService;
  private final TeamService teamService;

  public EntityCorrectionService(
      SynonymRepository synonymRepository,
      CompetitionService competitionService,
      TeamService teamService) {

    this.synonymRepository = synonymRepository;
    this.competitionService = competitionService;
    this.teamService = teamService;
  }

  public <T> void correctEntityFields(@NotNull T entity) throws ReflectiveOperationException {

    final Class<?> clazz = entity.getClass();
    final Field[] fields = ReflectionUtils.getAllFields(clazz);
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        final Method getter = getGetterMethod(clazz, field);
        final Object currentValue = getter.invoke(entity);

        validateCorrectedIsNotNull(field, currentValue);
        if (isCorrectable(field, currentValue)) {
          final Method setter = getSetterMethod(clazz, field);
          final Object correctedEntity = getCorrectedEntity(currentValue);
          setter.invoke(entity, correctedEntity);
        }
      }
    }
  }

  private void validateCorrectedIsNotNull(@NotNull Field field, Object fieldValue) {

    if (field.getAnnotation(Corrected.class) != null && fieldValue == null) {
      final String msg = String.format("Field: [%s] marked with @Corrected was null", field);
      throw new IllegalArgumentException(msg);
    }
  }

  private boolean isCorrectable(@NotNull Field field, Object fieldValue) {

    final boolean isCorrected = field.getAnnotation(Corrected.class) != null;
    final boolean isCorrectedOrNull =
        field.getAnnotation(CorrectedOrNull.class) != null && fieldValue != null;
    return isCorrected || isCorrectedOrNull;
  }

  @NotNull
  private Method getGetterMethod(@NotNull Class<?> clazz, @NotNull Field field)
      throws NoSuchMethodException {

    final String methodStub = getAccessorMethodStub(field);
    return clazz.getDeclaredMethod("get" + methodStub);
  }

  @NotNull
  private Method getSetterMethod(@NotNull Class<?> clazz, @NotNull Field field)
      throws NoSuchMethodException {

    final String methodStub = getAccessorMethodStub(field);
    return clazz.getDeclaredMethod("set" + methodStub, field.getType());
  }

  private @NotNull String getAccessorMethodStub(@NotNull Field field) {
    final String fieldName = field.getName();
    return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
  }

  public Collection<Synonym> addAllSynonyms(@NotNull Collection<Synonym> synonyms) {
    return synonymRepository.saveAll(synonyms);
  }

  public Synonym addSynonym(@NotNull Synonym synonym) {
    return synonymRepository.save(synonym);
  }

  public List<Synonym> getSynonymsFor(@NotNull String word) {
    return synonymRepository.findSynonymsByProperNameNameContains(word);
  }

  public <T> T getCorrectedEntity(@NotNull T entity) {

    final String name = getName(entity);
    return getEntityByName(entity, name)
        .or(
            () ->
                synonymRepository
                    .findSynonymByNameContains(name)
                    .flatMap(synonym -> getEntityByName(entity, synonym.getProperName().getName())))
        .orElse(entity);
  }

  private String getName(@NotNull Object o) {

    try {
      final Field field = o.getClass().getDeclaredField("name");
      final boolean accessible = field.canAccess(o);
      field.setAccessible(true);
      final Object value = field.get(o);
      field.setAccessible(accessible);
      if (value instanceof ProperName) {
        return ((ProperName) value).getName();
      }
      return (String) value;
    } catch (ReflectiveOperationException ignore) {
      // object does not have name field, or is not accessible
      return o.toString();
    }
  }

  @SuppressWarnings("unchecked cast")
  private <T> Optional<T> getEntityByName(@NotNull T entity, @NotNull String name) {
    if (entity instanceof Competition) {
      return (Optional<T>) competitionService.fetchCompetitionByName(name);
    } else if (entity instanceof Team) {
      return (Optional<T>) teamService.getTeamByName(name);
    } else {
      return Optional.empty();
    }
  }
}
