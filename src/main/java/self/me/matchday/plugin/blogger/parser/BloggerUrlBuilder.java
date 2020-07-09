package self.me.matchday.plugin.blogger.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class BloggerUrlBuilder {

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  protected final String baseUrl;
  protected LocalDateTime endDate;
  protected LocalDateTime startDate;
  protected boolean fetchBodies;
  protected boolean fetchImages;
  protected List<String> labels;
  protected int maxResults;
  protected String orderBy;
  protected String pageToken;
  protected String status;

  protected BloggerUrlBuilder(@NotNull final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public BloggerUrlBuilder endDate(LocalDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  public BloggerUrlBuilder startDate(LocalDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  public BloggerUrlBuilder fetchBodies(boolean fetchBodies) {
    this.fetchBodies = fetchBodies;
    return this;
  }

  public BloggerUrlBuilder fetchImages(boolean fetchImages) {
    this.fetchImages = fetchImages;
    return this;
  }

  public BloggerUrlBuilder labels(List<String> labels) {
    this.labels = labels;
    return this;
  }

  public BloggerUrlBuilder maxResults(int maxResults) {
    this.maxResults = maxResults;
    return this;
  }

  public BloggerUrlBuilder orderBy(String orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public BloggerUrlBuilder pageToken(String pageToken) {
    this.pageToken = pageToken;
    return this;
  }

  public BloggerUrlBuilder status(String status) {
    this.status = status;
    return this;
  }

  public abstract URL buildUrl() throws MalformedURLException;

}
