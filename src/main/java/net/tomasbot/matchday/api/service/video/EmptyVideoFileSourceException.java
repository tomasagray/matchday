/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.api.service.video;

import net.tomasbot.matchday.model.video.VideoFileSource;

public class EmptyVideoFileSourceException extends IllegalArgumentException {

  private static final String message = "VideoFileSource: %s has no video files";

  public EmptyVideoFileSourceException(VideoFileSource fileSource) {
    super(getMessage(fileSource));
  }

  private static String getMessage(VideoFileSource fileSource) {
    final String id = fileSource != null ? fileSource.getFileSrcId().toString() : "null";
    return String.format(message, id);
  }
}
