package self.me.matchday.plugin.datasource.blogger.parser.json;

import java.net.MalformedURLException;
import java.net.URL;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerUrlBuilder;

public class JsonUrlBuilder extends BloggerUrlBuilder {

  private static final String BASE_URL_PATTERN = "https://%s/feeds/posts/default?alt=json%s%s";
  private static final String LABEL_PATTERN = "&q=%s";
  private static final String DATE_PATTERN = "&updated-max=%s";

  JsonUrlBuilder(@NotNull String baseUrl) {
    super(baseUrl);
  }

  @Override
  public URL buildUrl() throws MalformedURLException {

    final String labelQuery = getLabelQuery(LABEL_PATTERN);
    // Only label OR date may be set at once
    final String dateQuery =
        (endDate == null || (labels != null && labels.size() > 0)) ? "" :
            String.format(DATE_PATTERN, endDate.format(DATE_TIME_FORMATTER));

    // Build the URL & return
    return
        new URL(String.format(BASE_URL_PATTERN, baseUrl, labelQuery, dateQuery));
  }
}
