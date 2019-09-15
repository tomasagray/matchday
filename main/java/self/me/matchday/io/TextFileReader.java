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

/** @author tomas */
public class TextFileReader {
  /**
   * Reads a remote text file from a web server via HTTP
   *
   * @param url the URL of the text file
   * @return the text data of the file
   * @throws IOException if the URL is invalid or the source cannot be read
   */
  @NotNull
  public static String readRemote(URL url) throws IOException {
    // File reader
    BufferedReader reader = null;

    try {
      // Container for text data
      StringBuilder sb = new StringBuilder();

      reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));

      // Add string data to container
      reader.lines().forEach(sb::append);
      return sb.toString();

    } catch (RuntimeException | IOException e) {
      // Catch and rethrow exception. A RuntimeException is
      // interpreted as a failure to connect.
      throw new IOException(
          "Stream read error <"
              + e.getClass().getCanonicalName()
              + ">: Could not find a file at the specified URL.");
    } finally {
      // Make sure the stream is closed
      if (reader != null) reader.close();
    }
  }

  /**
   * Reads a text file from the local file-system.
   *
   * @param uri a representing the Path to the file
   * @return the text data in the file
   * @throws IOException if the file cannot be read
   */
  @NotNull
  static String readLocal(Path uri) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(uri)) {
      // Container for the String data
      StringBuilder sb = new StringBuilder();
      // Add the data to the container
      reader.lines().forEach(sb::append);

      return sb.toString();
    }
  }
}
