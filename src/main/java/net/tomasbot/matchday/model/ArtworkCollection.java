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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Entity
@Getter
@Setter
public class ArtworkCollection {

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private final Set<Artwork> collection = new LinkedHashSet<>();

  private ArtworkRole role;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter private int selectedIndex;

  public ArtworkCollection() {
    this.role = null;
  }

  public ArtworkCollection(final ArtworkRole role) {
    this.role = role;
  }

  public Artwork get(int index) {
    return new ArrayList<>(collection).get(index);
  }

  public boolean setSelected(@NotNull Artwork artwork) {
    final int selectedIndex = collection.size();
    final boolean added = add(artwork);
    if (added) {
      setSelectedIndex(selectedIndex);
    } else {
      setSelectedIndex(indexOf(artwork));
    }
    return added;
  }

  public void setSelectedIndex(int index) {
    final int size = collection.size();
    if (index < 0 || (size > 0 && index >= size)) {
      throw new IndexOutOfBoundsException("Invalid selectedIndex: " + index);
    }
    this.selectedIndex = index;
  }

  public Artwork getSelected() {
    if (collection.isEmpty()) {
      return null;
    }
    return get(selectedIndex);
  }

  public Artwork getById(@NotNull Long artworkId) {
    return collection.stream()
        .filter(artwork -> artworkId.equals(artwork.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No artwork in this collection with ID: " + artworkId));
  }

  public int indexOf(Artwork t) {
    for (int i = 0; i < collection.size(); i++) {
      final Artwork ti = get(i);
      if (ti == null) {
        if (t == null) {
          return i;
        }
      } else if (ti.equals(t)) {
        return i;
      }
    }
    throw new IndexOutOfBoundsException("No such element: " + t);
  }

  public boolean add(Artwork artwork) {
    return collection.add(artwork);
  }

  public void addAll(Collection<Artwork> collection) {
    this.collection.addAll(collection);
  }

  public boolean remove(Artwork artwork) {
    return collection.remove(artwork);
  }

  public int size() {
    return collection.size();
  }

  @Override
  public String toString() {
    return String.format(
        "ArtworkCollection{id=%s, size=%d, selectedIndex=%d]", id, size(), selectedIndex);
  }
}
