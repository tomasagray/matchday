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

package net.tomasbot.matchday.db.converter;

import java.util.regex.Pattern;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class PatternConverter implements AttributeConverter<Pattern, String> {

  @Override
  public String convertToDatabaseColumn(Pattern attribute) {
    return attribute != null ? attribute.toString() : null;
  }

  @Override
  public Pattern convertToEntityAttribute(String dbData) {
    return dbData != null ? Pattern.compile(dbData, Pattern.UNICODE_CASE) : null;
  }
}
