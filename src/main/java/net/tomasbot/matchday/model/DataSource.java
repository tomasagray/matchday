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

package net.tomasbot.matchday.model;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.db.converter.UriConverter;

@Getter
@Setter
@Entity
@ToString
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class DataSource<T> {

  @Type(type = "java.lang.Class")
  private final Class<T> clazz;

  private final String title;

  @Convert(converter = UriConverter.class)
  private final URI baseUri;

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID dataSourceId;

  @Type(type = "uuid-char")
  private UUID pluginId;

  private boolean enabled = true;

  public DataSource() {
    this.title = null;
    this.baseUri = null;
    this.clazz = null;
  }

  public DataSource(@NotNull String title, @NotNull URI baseUri, @NotNull Class<T> clazz) {
    this.title = title;
    this.baseUri = baseUri;
    this.clazz = clazz;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DataSource<?> that)) return false;
    return isEnabled() == that.isEnabled()
        && Objects.equals(getDataSourceId(), that.getDataSourceId())
        && Objects.equals(getClazz(), that.getClazz())
        && Objects.equals(getTitle(), that.getTitle())
        && Objects.equals(getBaseUri(), that.getBaseUri())
        && Objects.equals(getPluginId(), that.getPluginId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getDataSourceId(), getClazz(), getTitle(), getBaseUri(), getPluginId(), isEnabled());
  }
}
