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

package self.me.matchday.plugin.datasource.blogger;

import java.util.stream.Stream;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.datasource.DataSourcePlugin;
import self.me.matchday.plugin.datasource.blogger.parser.PostParserFactory;

@Data
public abstract class BloggerParserPlugin implements DataSourcePlugin<Stream<Event>> {

  protected final BloggerPlugin bloggerPlugin;
  protected final PostParserFactory postParserFactory;

  protected Stream<Event> getEventStream(@NotNull final Blogger blogger) {
    return
        blogger
          .getPosts()
          .map(bloggerPost ->
              // Post parser factory supplied by subclasses
              getPostParserFactory()
                  .createParser(bloggerPost)
                  .getEvent());
  }
}
