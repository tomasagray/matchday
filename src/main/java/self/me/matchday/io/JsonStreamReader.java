/*
 * Copyright © 2020, Tomás Gray. All rights reserved.
 */

/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.io;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Class to read JSON text files, both local and remote. Returns the file as a String.
 *
 * @author tomas
 */
public class JsonStreamReader {

  private static final JsonParser parser = new JsonParser();

  /**
   * Read a remote JSON file from a URL via HTTP.
   *
   * @param url The URL of the JSON file/stream
   * @return JsonObject A JsonObject (Gson) representing the stream.
   * @throws IOException JSON cannot be read from the source.
   * @throws JsonParseException The loaded text is not valid JSON.
   * @throws JsonSyntaxException JSON is invalid.
   */
  public static JsonObject readRemote(URL url) throws IOException {
    // Read the file
    String json = TextFileReader.readRemote(url);
    // Ensure data read
    if ("".equals(json)) {
      throw new IOException("No data read from URL: " + url);
    }

    // Parse & return
    return parser.parse(json).getAsJsonObject();
  }

  /**
   * Reads a JSON (.json) file from the local filesystem.
   *
   * @param uri A String representing the Path to the .json file
   * @return JsonObject A Gson object representing the file
   * @throws IOException if the file cannot be read.
   */
  static JsonObject readLocal(Path uri) throws IOException {
    // Read the file
    String json = TextFileReader.readLocal(uri);
    // Ensure data read
    if ("".equals(json)) {
      throw new IOException("No data read from URL: " + uri);
    }
    // Parse & return
    return parser.parse(json).getAsJsonObject();
  }

  /**
   * Parses a JSON string into a JsonObject.
   *
   * @param json A JSON String
   * @return A JSON Object.
   */
  public static JsonObject readJsonString(String json) {
    return parser.parse(json).getAsJsonObject();
  }
}