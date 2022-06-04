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

package self.me.matchday.model.db.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.file.Path;

@Converter
public class PathConverter implements AttributeConverter<Path, String> {

  @Override
  public String convertToDatabaseColumn(Path attribute) {
    return attribute == null ? null : attribute.toString();
  }

  @Override
  public Path convertToEntityAttribute(String dbData) {
    return dbData == null ? null : Path.of(dbData);
  }
}
