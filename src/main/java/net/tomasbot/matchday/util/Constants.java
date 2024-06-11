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

package net.tomasbot.matchday.util;

import org.springframework.hateoas.LinkRelation;

public final class Constants {

  public static final LinkRelation EVENTS_REL = LinkRelation.of("events");
  public static final LinkRelation EMBLEM_REL = LinkRelation.of("emblem");
  public static final LinkRelation FANART_REL = LinkRelation.of("fanart");
}
