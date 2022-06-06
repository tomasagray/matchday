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
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ToString(callSuper = true)
@Entity
public final class PlaintextDataSource<T> extends DataSource<T> {

  @Getter
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private final List<PatternKit<?>> patternKits = new ArrayList<>();

  public PlaintextDataSource() {
    super();
  }

  public PlaintextDataSource(
      @NotNull String title,
      @NotNull URI baseUri,
      @NotNull Class<T> clazz,
      @NotNull List<PatternKit<?>> patternKits) {
    super(title, baseUri, clazz);
    this.patternKits.addAll(patternKits);
  }

  @SuppressWarnings("unchecked cast")
  public <S> List<PatternKit<? extends S>> getPatternKitsFor(@NotNull Class<S> clazz) {

    return patternKits.stream()
        .filter(patternKit -> patternKit.getClazz().equals(clazz))
        .map(patternKit -> (PatternKit<? extends S>) patternKit)
        .collect(Collectors.toList());
  }
}
