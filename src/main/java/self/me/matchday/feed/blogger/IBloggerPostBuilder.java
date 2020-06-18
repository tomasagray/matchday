/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.feed.blogger;

import com.google.gson.JsonObject;

public interface IBloggerPostBuilder {

  /**
   *  Create a <b>BloggerPost</b>
   *
   * @return A fully parsed BloggerPost object.
   */
  BloggerPost getBloggerPost();

}
