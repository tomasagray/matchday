/*
 * Copyright (c) 2020. 
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

package self.me.matchday.plugin.datasource.galataman;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.datasource.EventFileSourceParser;
import self.me.matchday.plugin.datasource.EventMetadataParser;
import self.me.matchday.plugin.datasource.EventSourceParser;
import self.me.matchday.plugin.datasource.blogger.BloggerPost;
import self.me.matchday.plugin.datasource.blogger.BloggerPostParser;

public class GalatamanPostParser extends BloggerPostParser {

  private final Event event;

  GalatamanPostParser(@NotNull final BloggerPost bloggerPost) {

    super(bloggerPost);
    // Instantiate parsers
    EventMetadataParser eventMetadataParser =
        new GalatamanEventMetadataParser(bloggerPost.getTitle());
    EventFileSourceParser fileSourceParser =
        new GManEventFileSourceParser(bloggerPost.getContent());
    // Get Event
    this.event = EventSourceParser.createEvent(eventMetadataParser, fileSourceParser);
  }

  @Override
  public Event getEvent() {
    return this.event;
  }
}
