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

import lombok.*;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.converter.CaseInsensitivePatternConverter;
import self.me.matchday.db.converter.PatternConverter;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "video_data_patterns")
public class VideoSourceMetadataPatternKit {

  @Id @GeneratedValue Long id;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private EventMetadataPatternKit eventPatternKit;

  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private List<FileSourceMetadataPatternKit> fileSourcePatternKits;

  private VideoSourceMetadataPatternKit(
      @NotNull EventMetadataPatternKit eventKit,
      @NotNull List<FileSourceMetadataPatternKit> fileSourceKits) {

    this.eventPatternKit = eventKit;
    this.fileSourcePatternKits = fileSourceKits;
  }

  @Contract("_, _ -> new")
  public static @NotNull VideoSourceMetadataPatternKit from(
      @NotNull EventMetadataPatternKit eventKit,
      @NotNull List<FileSourceMetadataPatternKit> fileSourceKits) {
    return new VideoSourceMetadataPatternKit(eventKit, fileSourceKits);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    VideoSourceMetadataPatternKit that = (VideoSourceMetadataPatternKit) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, eventPatternKit, fileSourcePatternKits);
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @Builder
  @Entity
  @Table(name = "event_data_patterns")
  public static class EventMetadataPatternKit {

    @Id @GeneratedValue private Long id;

    @Convert(converter = PatternConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private Pattern eventMetadataRegex;

    private int competitionName;
    private int homeTeamName;
    private int awayTeamName;
    private int season;
    private int fixture;
    private int date;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @ToString
  @Builder
  @Entity
  @Table(name = "filesource_data_patterns")
  public static class FileSourceMetadataPatternKit {

    @Id @GeneratedValue private Long id;

    @Convert(converter = PatternConverter.class)
    @Column(name = "filesource_regex", columnDefinition = "LONGTEXT")
    private Pattern fileSourceMetadataRegex;

    @Convert(converter = PatternConverter.class)
    private Pattern eventPartRegex;

    @Convert(converter = PatternConverter.class)
    private Pattern videoFileUrlRegex;

    @Convert(converter = CaseInsensitivePatternConverter.class)
    @Column(name = "part_id_regex")
    private Pattern eventPartIdentifierRegex;

    private int channel;
    private int resolution;
    private int source;
    private int duration;
    private int languages;
    private int container;
    private int videoCodec;
    private int videoBitrate;
    private int audioCodec;
    private int audioBitrate;
    private int filesize;
    private int framerate;
    private int audioChannels;
  }
}
