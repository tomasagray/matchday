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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class VideoSourceMetadataPatternKit {

  @Id @GeneratedValue private Long id;

  // event metadata
  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern competition;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern fixture;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern homeTeam;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern awayTeam;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern season;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern date;

  // video metadata
  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern startOfMetadata;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern metadataDelimiter;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern kvDelimiter;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern allMetadata;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern channel;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern source;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern approximateDuration;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern language;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern filesize;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern videoBitrate;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern resolution;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern framerate;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern container;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern videoCodec;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern audioBitrate;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern audioCodec;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern audioChannels;

  // video file identification
  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern videoFileUrl;

  @OneToOne(targetEntity = VideoSourceMetadataPattern.class)
  private VideoSourceMetadataPattern eventPartIdentifier;

  private long bitrateConversionFactor;
}
