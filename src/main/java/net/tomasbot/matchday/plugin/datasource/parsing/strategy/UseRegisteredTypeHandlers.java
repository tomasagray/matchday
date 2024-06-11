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

import java.util.List;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.plugin.datasource.parsing.CreationStrategy;
import net.tomasbot.matchday.plugin.datasource.parsing.TypeHandler;

public class UseRegisteredTypeHandlers implements CreationStrategy {

  private final List<TypeHandler<?>> typeHandlers;

  public UseRegisteredTypeHandlers(@NotNull final List<TypeHandler<?>> typeHandlers) {
    this.typeHandlers = typeHandlers;
  }

  @Override
  public Object apply(String data, Class<?> clazz) {
    return typeHandlers.stream()
        .filter(typeHandler -> typeHandler.getClazz().equals(clazz))
        .findFirst()
        .map(typeHandler -> typeHandler.getHandler().apply(data))
        .orElseThrow();
  }
}
