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

package self.me.matchday.model.video;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import self.me.matchday.db.converter.PatternConverter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.regex.Pattern;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class VideoSourceMetadataPatterns {

  @Id @GeneratedValue private Long id;

  // video metadata
  @Convert(converter = PatternConverter.class)
  private Pattern startOfMetadata;

  @Convert(converter = PatternConverter.class)
  private Pattern metadataDelimiter;

  @Convert(converter = PatternConverter.class)
  private Pattern kvDelimiter;

  @Convert(converter = PatternConverter.class)
  private Pattern language;

  @Convert(converter = PatternConverter.class)
  private Pattern filesize;

  @Convert(converter = PatternConverter.class)
  private Pattern channel;

  @Convert(converter = PatternConverter.class)
  private Pattern bitrate;

  @Convert(converter = PatternConverter.class)
  private Pattern resolution;

  @Convert(converter = PatternConverter.class)
  private Pattern framerate;

  @Convert(converter = PatternConverter.class)
  private Pattern container;

  // event metadata
  @Convert(converter = PatternConverter.class)
  private Pattern competition;

  @Convert(converter = PatternConverter.class)
  private Pattern fixture;

  @Convert(converter = PatternConverter.class)
  private Pattern teams;

  @Convert(converter = PatternConverter.class)
  private Pattern season;

  @Convert(converter = PatternConverter.class)
  private Pattern date;

  private long bitrateConversionFactor;
}
