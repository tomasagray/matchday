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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.awt.*;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import net.tomasbot.matchday.model.Setting;
import org.springframework.scheduling.support.CronTrigger;

public class JsonParser {

  private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss[.nnn][z]";
  private static final DateTimeFormatter DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern(DATETIME_PATTERN);
  private static final String DATE_PATTERN = "yyyy-MM-dd";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

  private static final Gson gson;

  static {
    gson =
        new GsonBuilder()
            .setPrettyPrinting()
            // === Matchday classes ===
            .registerTypeAdapter(
                Setting.class,
                (JsonSerializer<Setting<?>>)
                    (setting, type, context) -> {
                      Object data = setting.getData();
                      Class<?> clazz = data.getClass();
                      JsonElement settingJson = context.serialize(data);

                      JsonObject o = new JsonObject();
                      o.addProperty("type", clazz.getCanonicalName());
                      o.addProperty("path", setting.getPath().toString());
                      o.add("data", settingJson);
                      return o;
                    })
            .registerTypeAdapter(
                Setting.class,
                (JsonDeserializer<Setting<?>>)
                    (json, type, context) -> {
                      try {
                        JsonObject jo = json.getAsJsonObject();
                        String path = jo.get("path").getAsJsonPrimitive().getAsString();
                        Path key = Path.of(path);

                        String settingType = jo.get("type").getAsJsonPrimitive().getAsString();
                        Class<?> clazz = Class.forName(settingType);
                        Type classType = TypeToken.get(clazz).getType();

                        Object data = context.deserialize(jo.get("data"), classType);
                        return new Setting.GenericSetting<>(key, data);
                      } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                      }
                    })
            // === Java Standard Library ===
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
                CronTrigger.class,
                (JsonSerializer<CronTrigger>)
                    (cron, type, context) -> {
                      String data = cron.toString();
                      return context.serialize(data);
                    })
            .registerTypeAdapter(
                CronTrigger.class,
                (JsonDeserializer<CronTrigger>)
                    (json, type, ctx) -> {
                      String data = json.getAsJsonPrimitive().getAsString();
                      return new CronTrigger(data);
                    })
            .registerTypeAdapter(
                Duration.class,
                (JsonSerializer<Duration>)
                    (duration, type, context) -> {
                      JsonObject o = new JsonObject();
                      o.addProperty("seconds", duration.getSeconds());
                      o.addProperty("nanos", duration.getNano());
                      return o;
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
                LocalDate.class,
                (JsonSerializer<LocalDate>)
                    (date, type, context) -> {
                      String data = date.format(DATE_FORMATTER);
                      return new JsonPrimitive(data);
                    })
            .registerTypeAdapter(
                LocalDate.class,
                (JsonDeserializer<LocalDate>)
                    (json, type, context) -> {
                      if (json.isJsonObject()) {
                        JsonObject o = json.getAsJsonObject();
                        int year = o.get("year").getAsInt();
                        int month = o.get("month").getAsInt();
                        int day = o.get("day").getAsInt();
                        return LocalDate.of(year, month, day);
                      } else {
                        String s = json.getAsJsonPrimitive().getAsString();
                        return LocalDate.parse(s);
                      }
                    })
            .registerTypeAdapter(
                LocalDateTime.class,
                (JsonSerializer<LocalDateTime>)
                    (date, type, context) -> {
                      final String formatted = date.format(DATETIME_FORMATTER);
                      return new JsonPrimitive(formatted);
                    })
            .registerTypeAdapter(
                LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>)
                    (json, type, context) -> {
                      final String text = json.getAsJsonPrimitive().getAsString();
                      return LocalDateTime.parse(text, DATETIME_FORMATTER);
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
                    })
            .registerTypeAdapter(
                Pattern.class,
                (JsonSerializer<Pattern>)
                    (pattern, type, context) -> {
                      if (pattern == null) {
                        return null;
                      }
                      JsonObject o = new JsonObject();
                      o.addProperty("pattern", pattern.pattern());
                      o.addProperty("flags", pattern.flags());
                      return o;
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
