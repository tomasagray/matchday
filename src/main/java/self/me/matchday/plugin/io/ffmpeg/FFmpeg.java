package self.me.matchday.plugin.io.ffmpeg;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class FFmpeg {

  private static final String LOG_TAG = "FFMPEG";

  private static final String DATA_DIR = "data";
  private static final String MASTER_PL_NAME = "master.m3u8";
  private static final String SEGMENT_PL_NAME = "segments.m3u8";
  private static final String SEGMENT_PATTERN = "segment_%05d.ts";

  private final List<String> baseArgs;

  FFmpeg(@NotNull final String execPath) {
    this.baseArgs = List.of(
        String.format("\"%s\"", execPath),
        "-v quiet", "-y",
        "-protocol_whitelist concat,file,http,https,tcp,tls,crypto"
    );
  }

  FFmpegTask getStreamTask(@NotNull List<URI> uris, @NotNull final Path location)
      throws IOException {

    // Create data directory
    Files.createDirectories(Paths.get(location.toString(), DATA_DIR));

    // Create FFMPEG CLI command & return
    return
        new FFmpegTask(getTranscodeCmd(uris, location));
  }

  private @NotNull String getTranscodeCmd(@NotNull List<URI> uris,
      @NotNull final Path storageLocation) {

    final List<String> transcodeArgs = new ArrayList<>(baseArgs);
    final String storage = storageLocation.toString();

    // Assemble input string
    final List<String> uriStrings =
        uris
            .stream()
            .map(URI::toString)
            .collect(Collectors.toList());
    final String inputArg =
        String.format("-i \"concat:%s\"", String.join("|", uriStrings));
    final String segments =
        String.format("-hls_segment_filename \"%s\"",
            Paths.get(storage, DATA_DIR, SEGMENT_PATTERN));
    final String output =
        String.format("\"%s\"", Paths.get(storage, DATA_DIR, SEGMENT_PL_NAME));

    // Add arguments
    transcodeArgs.add(inputArg);
    transcodeArgs.add("-vcodec copy");
    transcodeArgs.add("-acodec copy");
    transcodeArgs.add("-muxdelay 0");
    transcodeArgs.add("-f hls");
    transcodeArgs.add(segments);
    transcodeArgs.add(output);

    // Collate & return
    return
        String.join(" ", transcodeArgs);
  }
}
