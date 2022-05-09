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

package self.me.matchday.plugin.datasource.parsing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import self.me.matchday.model.PatternKit;
import self.me.matchday.plugin.datasource.parsing.CreationStrategy.Priority;
import self.me.matchday.plugin.datasource.parsing.strategy.UseRegisteredTypeHandlers;
import self.me.matchday.plugin.datasource.parsing.strategy.UseStaticStringMethod;
import self.me.matchday.plugin.datasource.parsing.strategy.UseStringConstructor;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Stream;

@Component
public class TextParser {

  private final MultiValueMap<Priority, CreationStrategy> creationStrategies =
      new LinkedMultiValueMap<>();

  private TextParser(@NotNull List<TypeHandler<?>> handlers) {
    // register default strategies
    creationStrategies.add(Priority.HIGH, new UseRegisteredTypeHandlers(handlers));
    creationStrategies.add(Priority.NORMAL, new UseStaticStringMethod());
    creationStrategies.add(Priority.LOW, new UseStringConstructor());
  }

  public <E> Stream<? extends E> createEntityStreams(
      @NotNull Collection<PatternKit<? extends E>> patternKits, @NotNull final String data) {

    Stream<? extends E> base = Stream.empty();
    for (PatternKit<? extends E> patternKit : patternKits) {
      base = (Stream<? extends E>) Stream.concat(base, createEntityStream(patternKit, data));
    }
    return base;
  }

  public <E> Stream<? extends E> createEntityStream(
      @NotNull PatternKit<? extends E> patternKit, @NotNull String data) {

    final Stream.Builder<E> streamBuilder = Stream.builder();
    final Matcher matcher = patternKit.getPattern().matcher(data);
    while (matcher.find()) {
      try {
        final E e = parseEntity(patternKit, matcher);
        streamBuilder.add(e);
      } catch (ReflectiveOperationException ignore) {
        // nothing will be added to stream
      }
    }
    return streamBuilder.build().filter(Objects::nonNull);
  }

  @NotNull
  private <E> E parseEntity(@NotNull PatternKit<E> patternKit, Matcher matcher)
      throws ReflectiveOperationException {

    final Class<E> clazz = patternKit.getClazz();
    final E e = clazz.getConstructor().newInstance();
    patternKit
        .getFields()
        .forEach(
            (index, name) -> {
              try {
                final Field field = getFieldByName(e, name);
                if (field != null) {
                  final String group = matcher.group(index).trim();
                  final Object fieldValue = createFieldValue(field.getType(), group);
                  setFieldValue(e, field, fieldValue);
                }
              } catch (Exception ignore) {
                // fail silently; field value will be null
              }
            });
    return e;
  }

  @Nullable
  private Field getFieldByName(@NotNull Object obj, String name) {

    final Field[] fields = obj.getClass().getDeclaredFields();
    for (Field field : fields) {
      final Object value = getFieldValue(field, obj);
      if (field.getName().equals(name) && value == null) {
        return field;
      }
    }
    return null;
  }

  private @Nullable Object getFieldValue(@NotNull Field field, @NotNull Object obj) {

    try {
      final boolean canAccess = field.canAccess(obj);
      field.setAccessible(true);
      final Object value = field.get(obj);
      field.setAccessible(canAccess);
      return value;
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null;
    }
  }

  @Nullable
  private Object createFieldValue(Class<?> clazz, String data) {

    return creationStrategies.entrySet().stream()
        .flatMap(priorityLevel -> priorityLevel.getValue().stream())
        .map(creationStrategy -> applyCreationStrategy(creationStrategy, data, clazz))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private Object applyCreationStrategy(
      CreationStrategy creationStrategy, String data, Class<?> clazz) {

    try {
      return creationStrategy.apply(data, clazz);
    } catch (Throwable ignore) {
      return null;
    }
  }

  private void setFieldValue(@NotNull Object prototype, @NotNull Field field, Object value)
      throws IllegalAccessException {

    final boolean canAccess = field.canAccess(prototype);
    field.setAccessible(true);
    field.set(prototype, value);
    field.setAccessible(canAccess);
  }
}
