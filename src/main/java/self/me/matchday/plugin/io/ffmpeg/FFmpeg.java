package self.me.matchday.plugin.io.ffmpeg;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class FFmpeg {

  private static final String SEGMENT_PL_NAME = "playlist.m3u8";
  private static final String SEGMENT_PATTERN = "segment_%05d.ts";

  private final List<String> baseArgs;

  FFmpeg(@NotNull final String execPath) {
    this.baseArgs = List.of(
        String.format("\"%s\"", execPath),
        "-v quiet", "-y",
        "-protocol_whitelist concat,file,http,https,tcp,tls,crypto"
    );
  }

  FFmpegTask getHlsStreamTask(@NotNull List<URI> uris, @NotNull final Path location) {

    // Assemble arguments
    final String storage = location.toString();
    final List<String> transcodeArgs = new ArrayList<>();
    final List<String> uriStrings =
        uris
            .stream()
            .map(URI::toString)
            .collect(Collectors.toList());
    final String inputArg =
        String.format("-i \"concat:%s\"", String.join("|", uriStrings));
    final String segments =
        String.format("\"%s\"", Paths.get(storage, SEGMENT_PATTERN));

    // Add arguments
    transcodeArgs.add(inputArg);
    transcodeArgs.add("-vcodec copy");
    transcodeArgs.add("-acodec copy");
    transcodeArgs.add("-muxdelay 0");
    transcodeArgs.add("-f hls");
    transcodeArgs.add("-hls_playlist_type event");
    transcodeArgs.add("-hls_segment_filename");
    transcodeArgs.add(segments);

    // Create FFMPEG CLI command & return
    final FFmpegTask transcodeTask =
        new FFmpegTask(Strings.join(baseArgs, ' '), transcodeArgs);
    transcodeTask.setOutputFile(Paths.get(storage, SEGMENT_PL_NAME));
    return transcodeTask;
  }
}
