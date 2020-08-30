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

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.plugin.datasource.blogger;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents an individual, generic post on a Blogger blog. The constructor accepts either a Gson
 * JsonObject, or a JSON string.
 * <p>
 * This class can be extended to allow it to be customized to a particular blog.
 * </p>
 *
 * @author tomas
 */
@Data
@AllArgsConstructor
public class BloggerPost {

  // Fields
  private String bloggerPostID;
  private LocalDateTime published;
  private LocalDateTime lastUpdated;
  private List<String> categories;
  private String title;
  private String content;
  private String link;

  @Override
  public String toString() {

    // Ensure missing LocalDateTime fields do not cause NullPointerExceptions
    String nullSignifier = "{NULL}";
    String l_published = this.published != null ? this.published.toString() : nullSignifier;
    String l_updated = this.lastUpdated != null ? this.lastUpdated.toString() : nullSignifier;

    return "["
        + " id: "
        + this.bloggerPostID
        + " published: "
        + l_published
        + " updated: "
        + l_updated
        + " title: "
        + this.title
        + " link: "
        + this.link
        + " categories: "
        + this.categories
        + "]";
  }
}
