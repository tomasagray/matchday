package self.me.matchday.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.model.VideoMetadata;
import self.me.matchday.util.Log;

@Service
public class VideoMetadataService {

  private final static String LOG_TAG = "VideoMetadataService";
  private final static String FFPROBE_PATH = "\"C:/Program Files/ffmpeg/bin/ffprobe.exe\"";

  private final List<String> args;
  private final Gson gson;

  public VideoMetadataService() {

    // Setup global CLI arguments
    args = new ArrayList<>();
    args.add(FFPROBE_PATH);
    args.add("-v quiet");
    args.add("-print_format json");
    args.add("-show_streams");
    args.add("-show_format");
    args.add("-show_chapters");
    // Create Gson
    gson = new Gson();
  }

  /**
   * Fetches video file metadata using the local installation of FFMPEG.
   *
   * @param url The URL of the remote video file.
   * @return A VideoMetadata object.
   * @throws IOException If the data cannot be read, or the FFPROBE executable cannot be found.
   */
  public VideoMetadata readRemoteData(@NotNull final URL url) throws IOException {

    Log.i(LOG_TAG, String.format("Fetching metadata for file at URL: %s", url));

    // Result container
    String result;
    // Args for this job
    List<String> processArgs = new ArrayList<>(args);
    // Add remote URL to job args
    processArgs.add(url.toString());

    // Create process for job
    final String cmd = String.join(" ", processArgs);
    Process p = Runtime.getRuntime().exec(cmd);
    // Fetch remote data
    try (InputStream is = p.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
      // Read data and collect as a String
      result = br.lines().collect(Collectors.joining(""));
    } finally {
      // Ensure process closed
      p.destroy();
    }

    // Ensure JSON is valid
    try {
      return gson.fromJson(result, VideoMetadata.class);
    } catch (JsonSyntaxException e) {
      // Ensure invalid JSON is caught
      throw new IOException("Could not parse JSON from String", e);
    }
  }
}
