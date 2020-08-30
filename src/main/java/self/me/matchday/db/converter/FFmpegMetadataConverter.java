/*
 * Copyright (c) 2020. 
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

package self.me.matchday.db.converter;

import com.google.gson.Gson;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;

@Converter
public class FFmpegMetadataConverter implements AttributeConverter<FFmpegMetadata, String> {

  // Single Gson instance for all conversions
  private final Gson gson = new Gson();

  @Override
  public String convertToDatabaseColumn(FFmpegMetadata attribute) {
    return gson.toJson(attribute);
  }

  @Override
  public FFmpegMetadata convertToEntityAttribute(String dbData) {
    return gson.fromJson(dbData, FFmpegMetadata.class);
  }
}
