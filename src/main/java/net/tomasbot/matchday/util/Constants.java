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

  public static final class LinkRelations {
    // general links
    public static final LinkRelation DATA_REL = LinkRelation.of("data");
    public static final LinkRelation NEXT_LINK = LinkRelation.of("next");
    public static final LinkRelation FLAG_REL = LinkRelation.of("flag");
    public static final LinkRelation IMAGE_REL = LinkRelation.of("image");
    public static final LinkRelation METADATA_REL = LinkRelation.of("metadata");

    // event links
    public static final LinkRelation EVENTS_REL = LinkRelation.of("events");
    public static final LinkRelation MATCHES_REL = LinkRelation.of("matches");
    public static final LinkRelation HIGHLIGHTS_REL = LinkRelation.of("highlights");
    public static final LinkRelation COMPETITIONS_REL = LinkRelation.of("competitions");
    public static final LinkRelation TEAMS_REL = LinkRelation.of("teams");

    // video links
    public static final LinkRelation VIDEO_LINK_REL = LinkRelation.of("video");
    public static final LinkRelation PLAYLIST_REL = LinkRelation.of("stream");
    public static final LinkRelation STREAM_REL = LinkRelation.of("video-stream");
    public static final LinkRelation PREFERRED_PLAYLIST_REL = LinkRelation.of("preferred");

    // artwork links
    public static final LinkRelation ARTWORK_REL = LinkRelation.of("artwork");
    public static final LinkRelation EMBLEM_REL = LinkRelation.of("emblem");
    public static final LinkRelation FANART_REL = LinkRelation.of("fanart");
  }

  // NOTE: This property must be set statically and cannot be injected.
  // See 'src/main/resources/application.properties' -> server.servlet.contextPath
  public static final String API_VERSION = "v1";
  public static final String API_PREFIX = "/api/" + API_VERSION;
}
