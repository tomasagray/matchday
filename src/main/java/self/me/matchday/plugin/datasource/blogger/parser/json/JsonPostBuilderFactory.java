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

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class JsonPostBuilderFactory {

  /**
   * Create a <b>JsonBloggerPostBuilder</b> instance from a chunk of JSON.
   *
   * @param postJson The JSON representing the <b>BloggerPost</b>
   * @return A JsonBloggerPostBuilder object from which can be extracted a BloggerPost.
   */
  public JsonBloggerPostBuilder createPostBuilder(@NotNull final JsonObject postJson) {
    return
        new JsonBloggerPostBuilder(postJson);
  }
}
