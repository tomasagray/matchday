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
