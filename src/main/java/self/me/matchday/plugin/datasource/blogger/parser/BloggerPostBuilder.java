/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

package self.me.matchday.plugin.datasource.blogger.parser;

import self.me.matchday.plugin.datasource.blogger.BloggerPost;

public interface BloggerPostBuilder {

  /**
   *  Create a <b>BloggerPost</b>
   *
   * @return A fully parsed BloggerPost object.
   */
  BloggerPost getBloggerPost();

}