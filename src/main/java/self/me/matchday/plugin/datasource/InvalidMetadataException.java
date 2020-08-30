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

package self.me.matchday.plugin.datasource;

/**
 * Indicates the metadata under consideration could not be understood by its parser, so the parsing
 * process has broken down.
 */
public class InvalidMetadataException extends RuntimeException {

  private static final String DEFAULT_MSG = "Could not parse metadata!";

  public InvalidMetadataException() {
    super(DEFAULT_MSG);
  }

  public InvalidMetadataException(String msg) {
    super(msg);
  }

  public InvalidMetadataException(String msg, Exception e) {
    super(msg, e);
  }
}
