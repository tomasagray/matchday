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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public class ClassTypeAdapter extends TypeAdapter<Class<?>> {

  @Override
  public void write(JsonWriter jsonWriter, Class<?> clazz) throws IOException {
    if (clazz == null) {
      jsonWriter.nullValue();
      return;
    }
    jsonWriter.value(clazz.getName());
  }

  @Override
  public Class<?> read(@NotNull JsonReader jsonReader) throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.nextNull();
      return null;
    }
    try {
      return Class.forName(jsonReader.nextString());
    } catch (ClassNotFoundException exception) {
      throw new IOException(exception);
    }
  }
}
