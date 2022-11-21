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

package self.me.matchday.unit.plugin.datasource.parsing.fabric.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public class Marklar {

  private final String name;
  private final List<Bubble> bubbles = new ArrayList<>();
  private Foo foo;

  public Marklar(String name) {
    this.name = name;
  }

  public Foo getFoo() {
    return this.foo;
  }

  public void setFoo(Foo foo) {
    this.foo = foo;
  }

  public void addBubbles(List<? extends Bubble> bubbles) {
    this.bubbles.addAll(bubbles);
  }

  @Contract(pure = true)
  public @NotNull @UnmodifiableView List<Bubble> getBubbles() {
    return Collections.unmodifiableList(bubbles);
  }

  public String toString() {
    return String.format("Marklar[%s]'s Foo: %s;; Bubbles: %s", name, foo, bubbles);
  }
}
