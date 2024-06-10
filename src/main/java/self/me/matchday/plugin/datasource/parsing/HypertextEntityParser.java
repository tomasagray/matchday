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

package self.me.matchday.plugin.datasource.parsing;

import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;

@Component
public class HypertextEntityParser {

  private final List<DataSourceParser<?, ?>> parsers;

  public HypertextEntityParser(List<DataSourceParser<?, ?>> dataSourceParsers) {
    // register autowired parsers
    this.parsers = dataSourceParsers;
  }

  public <T, D> Stream<? extends T> getEntityStream(
      @NotNull DataSource<T> dataSource, @NotNull D data) {
    final DataSourceParser<T, D> parser = getParserForType(dataSource.getClazz());
    return parser.getEntityStream(dataSource, data);
  }

  @SuppressWarnings("unchecked cast")
  private <T, D> DataSourceParser<T, D> getParserForType(Class<T> clazz) {
    final String errMsg = "No entity parser registered for type: " + clazz;
    return (DataSourceParser<T, D>)
        this.parsers.stream()
            .filter(entry -> entry.getType().equals(clazz))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(errMsg));
  }
}
