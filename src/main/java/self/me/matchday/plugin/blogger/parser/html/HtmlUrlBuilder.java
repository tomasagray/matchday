package self.me.matchday.plugin.blogger.parser.html;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.blogger.parser.BloggerUrlBuilder;

public class HtmlUrlBuilder extends BloggerUrlBuilder {

  private static final String BASE_URL_PATTERN = "https://%s/search%s%s";
  private static final String LABEL_PATTERN = "/label/%s";
  private static final String DATE_PATTERN = "?updated-max=%s";

  HtmlUrlBuilder(@NotNull String baseUrl) {
    super(baseUrl);
  }

  @Override
  public URL buildUrl() throws MalformedURLException {

    // Concat labels
    final String label =
        (labels == null) ? "" :
            String.join("/", labels);
    // Format optional query strings
    final String labelQuery =
        "".equals(label) ? "" :
            String.format(LABEL_PATTERN, URLEncoder.encode(label, StandardCharsets.UTF_8));
    final String dateQuery =
        (endDate == null) ? "" :
            String.format(DATE_PATTERN, endDate.format(DATE_TIME_FORMATTER));

    // Build URL & return
    return new URL(String.format(BASE_URL_PATTERN, baseUrl, labelQuery, dateQuery));
  }
}
