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
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public interface EntityService<T, I> {

  T initialize(@NotNull T t);

  T save(@NotNull T entity);

  List<T> saveAll(@NotNull Iterable<? extends T> entities);

  Optional<T> fetchById(@NotNull I id);

  List<T> fetchAll();

  T update(@NotNull T entity);

  List<T> updateAll(@NotNull Iterable<? extends T> entities);

  void delete(@NotNull I id) throws IOException;

  void deleteAll(@NotNull Iterable<? extends T> entities) throws IOException;
}
