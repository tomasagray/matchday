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

import com.google.gson.*;

import java.awt.*;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class JsonParser {

  private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss[.nnn][z]";
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
  private static final String DATE_PATTERN = "yyyy-MM-dd";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

  private static final Gson gson;

  static {
    gson =
        new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(
                LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>)
                    (json, type, context) -> {
                      final String text = json.getAsJsonPrimitive().getAsString();
                      return LocalDateTime.parse(text, DATETIME_FORMATTER);
                    })
            .registerTypeAdapter(
                LocalDateTime.class,
                (JsonSerializer<LocalDateTime>)
                    (date, type, context) -> {
                        final String formatted = date.format(DATETIME_FORMATTER);
                        return new JsonPrimitive(formatted);
                })
            .registerTypeAdapter(
                LocalDate.class,
                (JsonDeserializer<LocalDate>)
                    (json, type, context) -> {
                        JsonObject o = json.getAsJsonObject();
                        int year = o.get("year").getAsInt();
                        int month = o.get("month").getAsInt();
                        int day = o.get("day").getAsInt();
                        return LocalDate.of(year, month, day);
                    })
            .registerTypeAdapter(
                LocalDate.class,
                (JsonSerializer<LocalDate>)
                    (date, type, context) -> {
                        String data = date.format(DATE_FORMATTER);
                        return new JsonPrimitive(data);
                    })
            .registerTypeAdapter(
                Duration.class,
                (JsonDeserializer<Duration>)
                    (json, type, context) -> {
                        JsonObject o = json.getAsJsonObject();
                        long seconds = o.get("seconds").getAsLong();
                        int nanos = o.get("nanos").getAsInt();
                        return Duration.ofSeconds(seconds).withNanos(nanos);
                    })
            .registerTypeAdapter(
                Color.class,
                (JsonSerializer<Color>)
                    (color, type, context) -> {
                        int red = color.getRed();
                        int green = color.getGreen();
                        int blue = color.getBlue();
                        JsonObject o = new JsonObject();
                        o.addProperty("red", red);
                        o.addProperty("green", green);
                        o.addProperty("blue", blue);
                        return o;
                    })
            .registerTypeAdapter(
                Color.class,
                (JsonDeserializer<Color>)
                    (json, type, context) -> {
                        JsonObject o = json.getAsJsonObject();
                        int red = o.get("red").getAsJsonPrimitive().getAsInt();
                        int green = o.get("green").getAsJsonPrimitive().getAsInt();
                        int blue = o.get("blue").getAsJsonPrimitive().getAsInt();
                        return new Color(red, green, blue);
                    })
            .registerTypeAdapter(
                Pattern.class,
                (JsonDeserializer<Pattern>)
                    (json, type, context) -> {
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
            .registerTypeHierarchyAdapter(
                Path.class,
                (JsonSerializer<Path>)
                  (path, type, context) -> {
                    if (path == null) {
                      return null;
                    }
                    return new JsonPrimitive(path.toString());
                  })
            .registerTypeHierarchyAdapter(
                Path.class,
                (JsonDeserializer<Path>)
                    (json, type, context) -> {
                      final String data = json.getAsJsonPrimitive().getAsString();
                      return Path.of(data);
                    }
            )
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
