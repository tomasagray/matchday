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

package net.tomasbot.matchday.plugin.datasource.parsing.strategy;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.plugin.datasource.parsing.CreationStrategy;

public class UseStaticStringMethod implements CreationStrategy {

  private static final String ERROR_MSG = "No static method with required params found";

  @Override
  public Object apply(String data, @NotNull final Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredMethods())
        .filter(method -> Modifier.isStatic(method.getModifiers()))
        .filter(
            method -> {
              final Class<?>[] parameterTypes = method.getParameterTypes();
              return parameterTypes.length == 1 && parameterTypes[0].equals(String.class);
            })
        .findAny()
        .map(
            method -> {
              try {
                method.setAccessible(true);
                return method.invoke(null, data);
              } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
              }
            })
        .orElseThrow(() -> new RuntimeException(ERROR_MSG));
  }
}
