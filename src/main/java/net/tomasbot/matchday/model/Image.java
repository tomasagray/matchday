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

package net.tomasbot.matchday.model;

import java.io.IOException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public record Image(byte[] data, MediaType contentType) {

  @Contract("_ -> new")
  public static @NotNull Image fromMultipartFile(@NotNull MultipartFile data) throws IOException {
    final String contentType = data.getContentType();
    if (contentType == null) {
      throw new IllegalArgumentException("Content-type was null");
    }
    return new Image(data.getBytes(), MediaType.valueOf(contentType));
  }

  @Override
  public String toString() {
    return String.format("Image [%s] - %d bytes", contentType, data.length);
  }
}
