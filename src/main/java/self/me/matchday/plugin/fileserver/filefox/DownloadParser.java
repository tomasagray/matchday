/*
 * Copyright (c) 2022.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

@Component
public class DownloadParser {

  private final ConnectionManager connectionManager;
  private final PageEvaluator pageEvaluator;

  public DownloadParser(
      @Autowired ConnectionManager connectionManager, @Autowired PageEvaluator pageEvaluator) {

    this.connectionManager = connectionManager;
    this.pageEvaluator = pageEvaluator;
  }

  public Optional<URL> parseDownloadRequest(
      @NotNull final URI uri, @NotNull MultiValueMap<String, String> cookieJar) {

    // Read remote page
    final ClientResponse downloadLandingResponse = connectionManager.get(uri, cookieJar);
    final String downloadLandingHtml = downloadLandingResponse.bodyToMono(String.class).block();
    final FileFoxPage page = pageEvaluator.getFileFoxPage(downloadLandingHtml);
    if (!(page instanceof FileFoxPage.DownloadLanding && page.isPremium())) {
      throw new FileFoxParsingException("Response from FileFox was not a Premium download page");
    }

    final FileFoxPage.DownloadLanding downloadLanding = (FileFoxPage.DownloadLanding) page;
    // Get hidden input fields
    final Map<String, String> queryParams = downloadLanding.getHiddenQueryParams();
    final URI hiddenFormUri = downloadLanding.getDdlSubmitUri();
    final URI formUri = uri.resolve(hiddenFormUri);

    // Fetch direct download page & parse
    final String directDownloadHtml =
        connectionManager.post(formUri, cookieJar, queryParams).bodyToMono(String.class).block();
    final FileFoxPage ddlPage = pageEvaluator.getFileFoxPage(directDownloadHtml);
    if (ddlPage instanceof FileFoxPage.DirectDownload) {
      final FileFoxPage.DirectDownload directDownload = (FileFoxPage.DirectDownload) ddlPage;
      return Optional.of(directDownload.getDdlUrl());
    }
    throw new FileFoxParsingException(
        "Not a DirectDownload page, or could not parse page: " + ddlPage.getText());
  }
}
