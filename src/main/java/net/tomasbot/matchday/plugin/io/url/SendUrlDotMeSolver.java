package net.tomasbot.matchday.plugin.io.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class SendUrlDotMeSolver implements UrlResolverPlugin {

  private static final Pattern URL_PATTERN = Pattern.compile("^https?://[\\w-.]*sendurl.me");

  public boolean canResolve(@NotNull URL url) {
    return URL_PATTERN.matcher(url.toString()).find();
  }

  /**
   * Returns the forward URL for a SendUrl.me URL, or the original URL if it's not a SendUrl.me URL.
   *
   * @param url The URL to check
   * @return The forward URL
   * @throws IOException If the URL cannot be opened
   */
  public URL resolveUrl(@NotNull URL url) throws IOException {
    if (canResolve(url)) {
      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // follow redirects
      try (InputStream is = connection.getInputStream()) {
        if (is.markSupported()) is.mark(0);

        // get updated URL after redirect
        return connection.getURL();
      } finally {
        if (connection != null) connection.disconnect();
      }
    }

    return url;
  }
}
