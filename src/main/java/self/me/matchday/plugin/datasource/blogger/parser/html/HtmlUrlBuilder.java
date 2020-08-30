/*
 * Copyright (c) 2020. 
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

package self.me.matchday.plugin.datasource.blogger.parser.html;

import java.net.MalformedURLException;
import java.net.URL;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerUrlBuilder;

public class HtmlUrlBuilder extends BloggerUrlBuilder {

  private static final String BASE_URL_PATTERN = "https://%s/search%s%s";
  private static final String LABEL_PATTERN = "/label/%s";
  private static final String DATE_PATTERN = "?updated-max=%s";

  HtmlUrlBuilder(@NotNull String baseUrl) {
    super(baseUrl);
  }

  @Override
  public URL buildUrl() throws MalformedURLException {

    final String labelQuery = getLabelQuery(LABEL_PATTERN);
    final String dateQuery =
        (endDate == null) ? "" :
            String.format(DATE_PATTERN, endDate.format(DATE_TIME_FORMATTER));

    // Build URL & return
    return new URL(String.format(BASE_URL_PATTERN, baseUrl, labelQuery, dateQuery));
  }

}
