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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class DownloadParser {

  private static final String HREF = "href";

  private final FileFoxPluginProperties pluginProperties;
  private final ConnectionManager connectionManager;
  private final PageEvaluator pageEvaluator;

  public DownloadParser(
      @Autowired FileFoxPluginProperties pluginProperties,
      @Autowired ConnectionManager connectionManager,
      @Autowired PageEvaluator pageEvaluator) {

    this.pluginProperties = pluginProperties;
    this.connectionManager = connectionManager;
    this.pageEvaluator = pageEvaluator;
  }

  public Optional<URL> parseDownloadRequest(
      @NotNull final URI uri, @NotNull MultiValueMap<String, String> cookieJar) {

    // Read remote page
    final ClientResponse downloadLandingResponse = connectionManager.get(uri, cookieJar);
    final String downloadLandingHtml = downloadLandingResponse.bodyToMono(String.class).block();
    final PageType pageType = pageEvaluator.getPageType(downloadLandingHtml);
    if (pageType != PageType.PremiumDownloadLanding) {
      throw new RuntimeException("Response from FileFox was not a Premium download page");
    }

    // Get hidden input fields
    final Map<String, String> queryParams = getHiddenQueryParams(downloadLandingHtml);
    final String hiddenFormUri = getHiddenFormUri(downloadLandingHtml);
    final URI formUri = uri.resolve(hiddenFormUri);

    // Fetch direct download page & parse
    final String directDownloadHtml =
        connectionManager.post(formUri, cookieJar, queryParams).bodyToMono(String.class).block();
    return parseDirectDownloadPage(directDownloadHtml);
  }

  private Map<String, String> getHiddenQueryParams(final String html) {

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

  private String getHiddenFormUri(final String pageHtml) {

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

  private Optional<URL> parseDirectDownloadPage(final String directDownloadHtml) {

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
    final Pattern urlPattern = pluginProperties.getDirectDownloadUrlPattern();
    return link.hasAttr(HREF) && urlPattern.matcher(link.attr(HREF)).find();
  }
}
