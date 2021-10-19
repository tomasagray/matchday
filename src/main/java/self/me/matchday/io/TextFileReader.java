/*
 * Copyright (c) 2021.
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
package self.me.matchday.io;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** A simple class for reading text files into a String. */
public class TextFileReader {

  /**
   * Reads a remote text file from a web server via HTTP
   *
   * @param url the URL of the text file
   * @return the text data of the file
   * @throws IOException if the URL is invalid, or the source cannot be read
   */
  @NotNull
  public static String readRemote(@NotNull final URL url) throws IOException {

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
      // Container for text data
      StringBuilder sb = new StringBuilder();
      // Add string data to container
      reader.lines().forEach(sb::append);
      return sb.toString();
    }
  }

  /**
   * Read text data from an InputStream.
   *
   * @param is InputStream representing the text data
   * @return A String
   * @throws IOException If the data cannot be read.
   */
  @NotNull
  public static String readStream(@NotNull final InputStream is) throws IOException {

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      // Result container
      StringBuilder sb = new StringBuilder();
      // Read data
      reader.lines().forEach(sb::append);
      return sb.toString();
    }
  }
}
