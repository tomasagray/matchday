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

package net.tomasbot.matchday.plugin.datasource.parsing.type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class LocalDateTimeHandler extends TypeHandler<LocalDateTime> {

  private static final List<String> patterns = new ArrayList<>();

  static {
    patterns.add("dd/MM/yyyy");
    patterns.add("MM/dd/yyyy");
  }

  public LocalDateTimeHandler() {
    super(LocalDateTime.class, LocalDateTimeHandler::parseData);
  }

  private static LocalDateTime parseData(String data) {
    return patterns.stream()
        .map(pattern -> attemptParsing(data, pattern))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Nullable
  private static LocalDateTime attemptParsing(String data, String pattern) {
    try {
      if (pattern.contains("T")) {
        return LocalDateTime.parse(data, DateTimeFormatter.ofPattern(pattern));
      }
      return LocalDate.parse(data, DateTimeFormatter.ofPattern(pattern)).atStartOfDay();
    } catch (Exception e) {
      return null;
    }
  }
}
