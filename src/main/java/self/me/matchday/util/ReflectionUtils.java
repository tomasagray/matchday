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

package self.me.matchday.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReflectionUtils {

  public static Field @NotNull [] getAllFields(@NotNull Class<?> clazz) {

    final Field[] allFields = {};
    final Field[] declaredFields = clazz.getDeclaredFields();
    final Field[] superClassFields = clazz.getSuperclass().getDeclaredFields();
    final List<Field> fieldContainer =
        new ArrayList<>(declaredFields.length + superClassFields.length);

    Collections.addAll(fieldContainer, declaredFields);
    Collections.addAll(fieldContainer, superClassFields);
    return fieldContainer.toArray(allFields);
  }
}
