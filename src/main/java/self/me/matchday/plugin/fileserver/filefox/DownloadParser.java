/*
 * Copyright (c) 2021.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.fileserver.filefox;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DownloadParser {

  private static final String HREF = "href";

  private final FileFoxPluginProperties pluginProperties;
  private final ConnectionManager connectionManager;

  public DownloadParser(
      @Autowired FileFoxPluginProperties pluginProperties,
      @Autowired ConnectionManager connectionManager) {

    this.pluginProperties = pluginProperties;
    this.connectionManager = connectionManager;
  }

  public Optional<URL> parseDownloadRequest(
      @NotNull final URI uri, @NotNull MultiValueMap<String, String> cookieJar)
      throws URISyntaxException {

    // Read remote page
    final ClientResponse downloadLandingResponse = connectionManager.get(uri, cookieJar);
    // Extract cookies
    downloadLandingResponse
        .cookies()
        .forEach(
            (name, cookieSet) ->
                cookieJar.put(
                    name,
                    cookieSet.stream().map(ResponseCookie::getValue).collect(Collectors.toList())));
    final String downloadLandingHtml = downloadLandingResponse.bodyToMono(String.class).block();
    validateDownloadPage(downloadLandingHtml);

    // Get hidden input fields
    assert downloadLandingHtml != null;
    final Map<String, String> queryParams = getHiddenQueryParams(downloadLandingHtml);
    final String hiddenFormUri = getHiddenFormUri(downloadLandingHtml);
    final URI formUri = uri.resolve(hiddenFormUri);

    // Fetch direct download page & parse
    final String directDownloadHtml =
        connectionManager.post(formUri, cookieJar, queryParams).bodyToMono(String.class).block();
    assert directDownloadHtml != null;
    return parseDirectDownloadPage(directDownloadHtml);
  }

  private void validateDownloadPage(@Nullable final String html) {

    // todo - extract Strings to plugin properties
    final String linkButtonText = "Get Download Link";
    if (html == null) {
      throw new RuntimeException("Download page data was null!");
    }
    final Document downloadPage = Jsoup.parse(html);
    if (downloadPage.select("ul.navbar-nav").text().contains("Login")) {
      throw new RuntimeException("User is not logged in");
    }
    if (downloadPage.text().contains("This file can be downloaded by Premium Members only")) {
      throw new RuntimeException("User account is not premium");
    }

    final Elements buttons = downloadPage.select("button.btn-default");
    // Find the "get download link" button
    buttons.stream()
        .filter(button -> button.text().equalsIgnoreCase(linkButtonText))
        .findAny()
        .orElseThrow(
            () -> new RuntimeException("HTML is not a valid download page (not logged in?)"));
  }

  private Map<String, String> getHiddenQueryParams(@NotNull final String html) {

    // Parse download page for hidden form
    final Document downloadPage = Jsoup.parse(html);
    final FormElement hiddenForm = downloadPage.getAllElements().forms().get(0);
    final Elements hiddenInputs = hiddenForm.select("input[type=hidden]");
    // Parse inputs
    final Map<String, String> hiddenValues = new LinkedHashMap<>();
    hiddenInputs.forEach(
        element -> {
          final String name = element.attr("name");
          final String value = element.attr("value");
          hiddenValues.put(name, value);
        });
    return hiddenValues;
  }

  private String getHiddenFormUri(@NotNull final String pageHtml) {

    final String errorMessage = "Could not parse direct download location";
    // Get form URL
    final Document document = Jsoup.parse(pageHtml);
    final Optional<Element> formOptional = document.getElementsByTag("form").stream().findFirst();
    if (formOptional.isPresent()) {
      final Element hiddenForm = formOptional.get();
      return hiddenForm.attr("action");
    }
    // else...
    throw new IllegalArgumentException(errorMessage);
  }

  private Optional<URL> parseDirectDownloadPage(@NotNull final String directDownloadHtml) {

    // Parse download link from response
    final Document document = Jsoup.parse(directDownloadHtml);
    // Check for download limit exceeded
    final Elements divs = document.select("div.info-box");
    divs.forEach(
        div -> {
          final String text = div.text();
          if (pluginProperties.getDownloadLimitPattern().matcher(text).find()) {
            throw new RuntimeException(text);
          }
        });
    final Elements links = document.getElementsByTag("a");
    return links.stream()
        .filter(this::isDirectDownloadLink)
        .findFirst()
        .map(element -> element.attr(HREF))
        .map(
            href -> {
              try {
                return new URL(href);
              } catch (MalformedURLException e) {
                throw new RuntimeException(e);
              }
            });
  }

  private boolean isDirectDownloadLink(@NotNull final Element link) {
    return link.hasAttr(HREF)
        && pluginProperties.getDirectDownloadUrlPattern().matcher(link.attr(HREF)).find();
  }
}
