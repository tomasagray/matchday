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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PageEvaluator {

  private static final String NAVBAR_SELECTOR = "ul.navbar-nav";
  private static final String BUTTON_SELECTOR = "button.btn-default";
  private static final String HIDDEN_INPUT = "input[type=hidden]";
  private static final String HREF = "href";

  private final FileFoxPluginProperties pluginProperties;

  PageEvaluator(@Autowired FileFoxPluginProperties pluginProperties) {
    this.pluginProperties = pluginProperties;
  }

  public FileFoxPage getFileFoxPage(final String html) {
    try {
      return getPageType(html);
    } catch (Throwable e) {
      return new FileFoxPage.Invalid();
    }
  }

  private FileFoxPage getPageType(final String html) throws URISyntaxException {

    final Document page = Jsoup.parse(html);
    final String pageText = page.text();
    final Elements navBar = page.select(NAVBAR_SELECTOR);
    final Elements buttons = page.select(BUTTON_SELECTOR);
    final Matcher dlLimit = pluginProperties.getDownloadLimitPattern().matcher(pageText);
    final Optional<Element> ddlSubmitButton =
            buttons.stream()
                    .filter(button -> button.text().equalsIgnoreCase(pluginProperties.getLinkButtonText()))
                    .findAny();
    final Optional<URL> ddlUrlOptional = getDirectDownloadUrl(page);

    if (navBar.text().contains(pluginProperties.getLoggedOutText())) {
      return new FileFoxPage.Login();
    }
    if (pageText.contains(pluginProperties.getPremiumOnlyError())) {
      return FileFoxPage.DownloadLanding.builder().premium(false).loggedIn(true).build();
    }
    if (dlLimit.find()) {
      final FileFoxPage.Invalid invalidPage = new FileFoxPage.Invalid();
      invalidPage.setError(dlLimit.group());
      return invalidPage;
    }
    if (ddlSubmitButton.isPresent()) {
      final Map<String, String> queryParams = getHiddenQueryParams(page);
      final URI ddlSubmitUri = getHiddenFormUri(page);
      return FileFoxPage.DownloadLanding.builder()
              .hiddenQueryParams(queryParams)
              .ddlSubmitUri(ddlSubmitUri)
              .loggedIn(true)
              .premium(true)
              .build();
    }
    if (ddlUrlOptional.isPresent()) {
      final URL ddlUrl = ddlUrlOptional.get();
      return FileFoxPage.DirectDownload.builder()
              .ddlUrl(ddlUrl)
              .build();
    }
    // Default
    return new FileFoxPage.Invalid();
  }

  private Map<String, String> getHiddenQueryParams(final Document document) {

    final FormElement hiddenForm = document.getAllElements().forms().get(0);
    final Elements hiddenInputs = hiddenForm.select(HIDDEN_INPUT);
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

  private URI getHiddenFormUri(final Document document) throws URISyntaxException {

    final Optional<Element> formOptional = document.getElementsByTag("form").stream().findFirst();
    if (formOptional.isPresent()) {
      final Element hiddenForm = formOptional.get();
      final String url = hiddenForm.attr("action");
      return new URI(url);
    }
    // else...
    throw new IllegalArgumentException(pluginProperties.getDdlFormErrorText());
  }

  @NotNull
  private Optional<URL> getDirectDownloadUrl(Document document) {

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
