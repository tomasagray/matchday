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

/** Indicates the parser encountered an irrecoverable error when examining this Blogger plugin. */
public class InvalidBloggerFeedException extends RuntimeException {

  private static final String DEFAULT_MSG = "Could not parse Blogger from data given";

  public InvalidBloggerFeedException() {
    super(DEFAULT_MSG);
  }

  public InvalidBloggerFeedException(final String msg) {
    super(msg);
  }

  public InvalidBloggerFeedException(String msg, Exception e) {
    super(msg, e);
  }
}
