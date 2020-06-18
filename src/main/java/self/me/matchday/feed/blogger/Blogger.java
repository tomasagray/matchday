/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.feed.blogger;

import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Blogger blog. Immutable.
 *
 * @author tomas
 */
@Getter
@AllArgsConstructor
public class Blogger {

  // Fields
  private final String blogId;
  private final String title;
  private final String version;
  private final String author;
  private final String link;
  private final Stream<BloggerPost> posts;
  private final long postCount;

  @NotNull
  @Override
  public String toString() {
    return 
        "[ "
        + "Title: "
        + getTitle()
        + " Version: "
        + getVersion()
        + " Author: "
        + getAuthor()
        + " Link: "
        + getLink()
        + " Posts: "
        + getPostCount()
        + "]";
  }
}
