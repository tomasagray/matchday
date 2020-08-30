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

package self.me.matchday.plugin.datasource.blogger.parser.json;

import java.net.URL;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilder;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilderFactory;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerUrlBuilder;

public class JsonBuilderFactory implements BloggerBuilderFactory {

  @Override
  public BloggerBuilder getBloggerBuilder(@NotNull URL url) {

    return
        new JsonBloggerBuilder(url, new JsonPostBuilderFactory());
  }

  @Override
  public BloggerUrlBuilder getBloggerUrlBuilder(@NotNull final String baseUrl) {

    return
        new JsonUrlBuilder(baseUrl);
  }
}
