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

package self.me.matchday.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import self.me.matchday.db.converter.UriConverter;
import self.me.matchday.plugin.datasource.parsing.PatternKit;

import javax.persistence.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
@Entity
@RequiredArgsConstructor
public final class DataSource {

  @Id @GeneratedValue private Long id;
  private final UUID pluginId;

  @Convert(converter = UriConverter.class)
  private final URI baseUri;

  @ManyToMany(targetEntity = PatternKit.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private final List<PatternKit<?>> patternKits;

  private boolean enabled = true;

  public DataSource() {
    this.baseUri = null;
    this.patternKits = null;
    this.pluginId = null;
  }
}
