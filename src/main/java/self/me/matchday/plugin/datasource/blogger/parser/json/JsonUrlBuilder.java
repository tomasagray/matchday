package self.me.matchday.plugin.datasource.blogger.parser.json;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerUrlBuilder;

public class JsonUrlBuilder extends BloggerUrlBuilder {

  private static final String BASE_URL_PATTERN = "https://%s/feeds/posts/default%s?alt=json%s";
  private static final String LABEL_PATTERN = "/-/%s";
  private static final String DATE_PATTERN = "&updated-max=%s";

  JsonUrlBuilder(@NotNull String baseUrl) {
    super(baseUrl);
  }

  @Override
  public URL buildUrl() throws MalformedURLException {

    // Concat labels
    final String label =
        (labels == null) ? "" :
            String.join("/", labels);
    // Format query strings
    final String labelQuery =
        "".equals(label) ? "" :
            String.format(LABEL_PATTERN, URLEncoder.encode(label, StandardCharsets.UTF_8));
    // Only label OR date may be set at once
    final String dateQuery =
        (endDate == null || (labels != null && labels.size() > 0)) ? "" :
            String.format(DATE_PATTERN, endDate.format(DATE_TIME_FORMATTER));

    // Build the URL & return
    return
        new URL(String.format(BASE_URL_PATTERN, baseUrl, labelQuery, dateQuery));
  }
}
