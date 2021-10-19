/*
 * Copyright (c) 2021.
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

package self.me.matchday.plugin.datasource.blogger.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BloggerEntry {

  private BloggerFeed.Generic<String> id;
  private BloggerFeed.Generic<LocalDateTime> published;
  private BloggerFeed.Generic<LocalDateTime> updated;
  private List<BloggerFeed.Term> category;
  private BloggerFeed.Str title;
  private BloggerFeed.Str content;
  private List<BloggerFeed.Link> link;
  private List<BloggerFeed.Author> author;
  private BloggerFeed.Generic<Integer> thr$total;
}
