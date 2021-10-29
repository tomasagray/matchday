/*
 * Copyright (c) 2021.
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
import self.me.matchday.Corrected;
import self.me.matchday.CorrectedOrNull;
import self.me.matchday.db.SynonymRepository;
import self.me.matchday.model.Synonym;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@Service
public class EntityCorrectionService {

  private final SynonymRepository synonymRepository;

  public EntityCorrectionService(SynonymRepository synonymRepository) {
    this.synonymRepository = synonymRepository;
  }

  public <T> void correctFields(@NotNull T value) throws ReflectiveOperationException {

    final Class<?> clazz = value.getClass();
    for (Field field : clazz.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        final Method getter = getGetterMethod(clazz, field);
        final Object fieldValue = getter.invoke(value);

        validateCorrectedIsNotNull(field, fieldValue);
        if (field.getType() == String.class) {
          final String strVal = (String) fieldValue;
          correctStringWithSynonym(value, field, strVal);
        } else if (isCorrectable(field, fieldValue)) {
          correctFields(fieldValue);
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

  private <T> void correctStringWithSynonym(@NotNull T value, @NotNull Field field, String strVal)
      throws IllegalAccessException {

    final Synonym synonym = this.getSynonymsFor(strVal);
    final String primarySynonym = synonym != null ? synonym.getPrimary() : strVal;
    final boolean accessState = field.canAccess(value);
    field.setAccessible(true);
    field.set(value, primarySynonym);
    field.setAccessible(accessState);
  }

  @NotNull
  private Method getGetterMethod(@NotNull Class<?> clazz, @NotNull Field field)
      throws NoSuchMethodException {
    final String fieldName = field.getName();
    final String methodNameStub = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    return clazz.getDeclaredMethod("get" + methodNameStub);
  }

  public Synonym createSynonymsFrom(@NotNull List<String> synonyms) {
    return this.addSynonym(new Synonym(synonyms));
  }

  public Synonym addSynonym(@NotNull Synonym synonym) {
    return synonymRepository.saveAndFlush(synonym);
  }

  public Synonym getSynonymsFor(@NotNull String word) {
    return synonymRepository.findAll().stream()
        .filter(synonym -> synonym.getSynonyms().contains(word))
        .findAny()
        .orElse(null);
  }
}
