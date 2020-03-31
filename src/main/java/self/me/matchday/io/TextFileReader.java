/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

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
  public static String readRemote(URL url) throws IOException {

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
   * Reads a text file from the local file-system.
   *
   * @param uri a representing the Path to the file
   * @return the text data in the file
   * @throws IOException if the file cannot be read.
   */
  @NotNull
  public static String readLocal(Path uri) throws IOException {

    try (BufferedReader reader = Files.newBufferedReader(uri)) {
      // Container for the String data
      StringBuilder sb = new StringBuilder();
      // Add the data to the container
      reader.lines().forEach(sb::append);
      return sb.toString();
    }
  }
}