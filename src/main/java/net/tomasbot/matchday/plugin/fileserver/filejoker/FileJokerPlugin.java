package net.tomasbot.matchday.plugin.fileserver.filejoker;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.tomasbot.matchday.model.FileServerUser;
import net.tomasbot.matchday.plugin.fileserver.FileServerPlugin;
import net.tomasbot.matchday.util.HttpConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;

@Component
public class FileJokerPlugin implements FileServerPlugin {

  private static final Pattern PCT_PATTERN = Pattern.compile("([\\d.]+)%");

  private final FileJokerPluginProperties pluginProperties;
  private final HttpConnectionManager connectionManager;

  public FileJokerPlugin(
      HttpConnectionManager connectionManager, FileJokerPluginProperties pluginProperties) {
    this.connectionManager = connectionManager;
    this.pluginProperties = pluginProperties;
  }

  /**
   * Parse raw HTML data for the user's remaining bandwidth
   *
   * @param profileData raw HTML String data
   * @return remaining bandwidth as a decimal percentage
   * @throws IOException if the data cannot be parsed
   */
  private float parseBandwidthData(String profileData) throws IOException {
    Document doc = Jsoup.parse(profileData);
    Elements progressBars = doc.select(pluginProperties.getBandwidthSelector());

    if (progressBars.isEmpty()) throw new IOException("Could not parse FileJoker profile data");

    // parse the first progress bar
    Element bandwidthBar = progressBars.get(0);
    String bandwidthText = bandwidthBar.text();
    Matcher matcher = PCT_PATTERN.matcher(bandwidthText);

    if (matcher.find()) {
      float bandwidth = Float.parseFloat(matcher.group(1));
      return bandwidth / 100;
    }

    return 0.0f;
  }

  private String getDirectDownloadPage(@NotNull Collection<HttpCookie> cookies, String pageData)
      throws IOException {
    if (pageData == null) throw new IOException("FileJoker page data was null");

    Document doc = Jsoup.parse(pageData);
    Elements downloadForms = doc.select(pluginProperties.getDownloadSelector());

    if (downloadForms.isEmpty()) throw new IOException("Could not parse FileJoker download URL");

    Element downloadForm = downloadForms.get(0);
    String formUrl = downloadForm.attr("action");

    // get hidden query params
    final Map<String, String> queryParams = new HashMap<>();
    downloadForm
        .select("input[type=hidden]")
        .forEach(input -> queryParams.put(input.attr("name"), input.attr("value")));

    URI formUri = getFormUri(formUrl);
    ClientResponse response = connectionManager.post(formUri, cookies, queryParams);
    return response.bodyToMono(String.class).block();
  }

  private @NotNull URI getFormUri(@NotNull String formUrl) throws IOException {
    try {
      URI uri = URI.create(formUrl);

      // ensure absolute URI
      if (!uri.isAbsolute()) {
        URI baseUri = pluginProperties.getBaseUrl().toURI();
        uri = baseUri.resolve(uri);
      }

      return uri;
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private @NotNull String getDirectDownloadUrl(String pageData) throws IOException {
    String downloadSelector = pluginProperties.getDirectDownloadSelector();

    Document doc = Jsoup.parse(pageData);
    Elements downloadLinks = doc.select(downloadSelector);

    if (downloadLinks.isEmpty()) throw new IOException("Could not parse direct download URL");

    // get first, and should be only download link
    return downloadLinks.get(0).attr("href");
  }

  @Override
  public @NotNull ClientResponse login(@NotNull FileServerUser user) throws IOException {
    throw new IOException("Not implemented");
  }

  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url, @NotNull Set<HttpCookie> cookies)
      throws IOException {
    try {
      ClientResponse response = connectionManager.get(url.toURI(), cookies);
      String pageData = response.bodyToMono(String.class).block();
      if (pageData == null) throw new IOException("Could not retrieve FileJoker page data");

      String directDownloadPage = getDirectDownloadPage(cookies, pageData);
      String ddlUrl = getDirectDownloadUrl(directDownloadPage);

      return Optional.of(new URL(ddlUrl));
    } catch (Throwable e) {
      if (e instanceof IOException ioe) throw ioe;
      else throw new IOException(e);
    }
  }

  @Override
  public float getRemainingBandwidth(@NotNull Set<HttpCookie> cookies) throws IOException {
    try {
      URI profile = pluginProperties.getProfileUrl().toURI();
      ClientResponse response = connectionManager.get(profile, cookies);
      String profileData = response.bodyToMono(String.class).block();

      if (profileData == null) throw new IOException("Could not retrieve FileJoker profile data");

      return parseBandwidthData(profileData);

    } catch (Throwable e) {
      throw new IOException(e);
    }
  }

  @Override
  public @NotNull URL getHostname() {
    return pluginProperties.getBaseUrl();
  }

  @Override
  public @NotNull Duration getRefreshRate() {
    return Duration.ofHours(pluginProperties.getRefreshHours());
  }

  @Override
  public boolean acceptsUrl(@NotNull URL url) {
    final Matcher urlMatcher = pluginProperties.getLinkUrlPattern().matcher(url.toString());
    return urlMatcher.find();
  }

  @Override
  public UUID getPluginId() {
    return UUID.fromString(pluginProperties.getId());
  }

  @Override
  public String getTitle() {
    return pluginProperties.getTitle();
  }

  @Override
  public String getDescription() {
    return pluginProperties.getDescription();
  }
}
