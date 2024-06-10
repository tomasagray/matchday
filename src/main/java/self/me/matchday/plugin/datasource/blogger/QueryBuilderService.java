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

package self.me.matchday.plugin.datasource.blogger;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.blogger.BloggerPlugin.SourceType;

@Service
public class QueryBuilderService {

  public @NotNull String buildQueryFrom(
      @NotNull final SnapshotRequest request, @NotNull SourceType type) {
    final BloggerQueryBuilder queryBuilder =
        type == SourceType.JSON
            ? new BloggerQueryBuilder.JsonQueryBuilder(request)
            : new BloggerQueryBuilder.HtmlQueryBuilder(request);
    return queryBuilder.build();
  }
}
