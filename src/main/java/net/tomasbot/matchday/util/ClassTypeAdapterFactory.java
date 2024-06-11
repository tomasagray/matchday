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

package net.tomasbot.matchday.util;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unchecked")
public class ClassTypeAdapterFactory implements TypeAdapterFactory {

  private final TypeAdapter<?> typeAdapter;

  public ClassTypeAdapterFactory(TypeAdapter<?> typeAdapter) {
    this.typeAdapter = typeAdapter;
  }

  @Override
  public <T> TypeAdapter<T> create(Gson gson, @NotNull TypeToken<T> typeToken) {
    if (!Class.class.isAssignableFrom(typeToken.getRawType())) {
      return null;
    }
    return (TypeAdapter<T>) this.typeAdapter;
  }
}
