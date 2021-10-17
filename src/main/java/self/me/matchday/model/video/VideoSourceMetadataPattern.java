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

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.converter.PatternConverter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Entity
public class VideoSourceMetadataPattern {

  @Convert(converter = PatternConverter.class)
  private final Pattern pattern;

  @Setter @Id @GeneratedValue private Long id;

  public VideoSourceMetadataPattern() {
    this.pattern = null;
  }

  public String getDataFrom(@NotNull final String string) {

    String result = null;
    assert pattern != null;
    final Matcher matcher = pattern.matcher(string);
    if (matcher.find() && matcher.groupCount() > 0) {
      final String group = matcher.group(1);
      result = group.trim();
    }
    return result;
  }

  @Override
  public String toString() {
    return pattern != null ? pattern.toString() : null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    VideoSourceMetadataPattern that = (VideoSourceMetadataPattern) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    final int idCode = (id != null) ? id.hashCode() : 31;
    final int patternCode = pattern != null ? pattern.hashCode() : Pattern.compile("").hashCode();
    return idCode * patternCode;
  }
}
