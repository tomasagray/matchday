/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.util.Log;

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
