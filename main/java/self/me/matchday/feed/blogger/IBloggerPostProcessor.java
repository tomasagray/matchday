/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger;

import com.google.gson.JsonObject;

public interface IBloggerPostProcessor {

  /**
   *  Parse a chunk of JSON extracted from a <b>Blogger</b> blog and return it as a <b>BloggerPost</b>
   *
   * @param entry A JSON object representing a single Blogger blog entry
   * @return A fully parsed BloggerPost object.
   */
  BloggerPost parse(JsonObject entry);
}
