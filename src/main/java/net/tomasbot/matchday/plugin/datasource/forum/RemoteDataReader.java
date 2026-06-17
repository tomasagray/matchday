package net.tomasbot.matchday.plugin.datasource.forum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public final class RemoteDataReader {

  private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([\\w-]+)");
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static Charset determineCharset(@NotNull URL url) {
    try {
      final URLConnection conn = url.openConnection();
      String contentType = conn.getContentType();

      Matcher matcher = CHARSET_PATTERN.matcher(contentType);
      if (matcher.find()) {
        String charset = matcher.group(1);
        return Charset.forName(charset);
      }
    } catch (Throwable ignore) {
      // do nothing
    }

    // if charset could not be determined
    return DEFAULT_CHARSET;
  }

  public static String readDataFrom(@NotNull URL url) throws IOException {
    Charset charset = determineCharset(url);
    try (InputStreamReader in = new InputStreamReader(url.openStream(), charset);
        BufferedReader reader = new BufferedReader(in)) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }
}
