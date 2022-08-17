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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class JsonParser {

  private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss[.nnn][z]";
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

  private static final Gson gson;

  static {
    gson =
        new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(
                LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>)
                    (json, type, jsonDeserializationContext) -> {
                      final String text = json.getAsJsonPrimitive().getAsString();
                      return LocalDateTime.parse(text, FORMATTER);
                    })
            .registerTypeAdapter(
                Pattern.class,
                (JsonDeserializer<Pattern>)
                    (json, type, jsonDeserializationContext) -> {
                      final JsonObject jsonObject = json.getAsJsonObject();
                      final String pattern =
                          jsonObject.get("pattern").getAsJsonPrimitive().getAsString();
                      int flags = 0;
                      final JsonElement flagData = jsonObject.get("flags");
                      if (flagData != null) {
                        flags = flagData.getAsJsonPrimitive().getAsInt();
                      }
                      //noinspection MagicConstant
                      return Pattern.compile(pattern, flags);
                    })
            .registerTypeAdapterFactory(new ClassTypeAdapterFactory(new ClassTypeAdapter()))
            .create();
  }

  public static String toJson(Object src) {
    return gson.toJson(src);
  }

  public static String toJson(Object src, Type type) {
    return gson.toJson(src, type);
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    return gson.fromJson(json, clazz);
  }

  public static <T> T fromJson(Reader reader, Class<T> clazz) {
    return gson.fromJson(reader, clazz);
  }

  public static <T> T fromJson(String json, Type type) {
    return gson.fromJson(json, type);
  }

  public static <T> T fromJson(Reader reader, Type type) {
    return gson.fromJson(reader, type);
  }
}
