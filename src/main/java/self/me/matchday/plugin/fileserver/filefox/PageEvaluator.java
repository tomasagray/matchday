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

import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PageEvaluator {

  private static final String NAVBAR_SELECTOR = "ul.navbar-nav";
  private static final String BUTTON_SELECTOR = "button.btn-default";

  private final FileFoxPluginProperties pluginProperties;

  PageEvaluator(@Autowired FileFoxPluginProperties pluginProperties) {
    this.pluginProperties = pluginProperties;
  }

  public PageType getPageType(@Nullable final String html) {

    if (html == null) {
      return PageType.Invalid;
    }

    final Document page = Jsoup.parse(html);
    final Elements navBar = page.select(NAVBAR_SELECTOR);
    final Elements buttons = page.select(BUTTON_SELECTOR);
    final Optional<Element> ddlSubmitButton =
        buttons.stream()
            .filter(button -> button.text().equalsIgnoreCase(pluginProperties.getLinkButtonText()))
            .findAny();

    if (navBar.text().contains(pluginProperties.getLoggedOutText())) {
      return PageType.Login;
    }
    if (page.text().contains(pluginProperties.getPremiumOnlyError())) {
      return PageType.FreeDownloadLanding;
    }
    if (ddlSubmitButton.isPresent()) {
      return PageType.PremiumDownloadLanding;
    }
    return PageType.Invalid;
  }
}
