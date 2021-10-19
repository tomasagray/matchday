/*
 * Copyright (c) 2021.
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

package self.me.matchday.plugin.datasource.blogger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.model.Blogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonBloggerParser implements BloggerParser {

  private static final Gson gson;

  static {
    gson =
        new GsonBuilder()
            .registerTypeAdapter(
                LocalDateTime.class,
                (JsonDeserializer<LocalDateTime>)
                    (json, type, jsonDeserializationContext) ->
                        LocalDateTime.parse(
                            json.getAsString(), DateTimeFormatter.ofPattern(DATETIME_PATTERN)))
            .create();
  }

  @Override
  public Blogger getBlogger(@NotNull final String data) {
    return gson.fromJson(data, Blogger.class);
  }
}
