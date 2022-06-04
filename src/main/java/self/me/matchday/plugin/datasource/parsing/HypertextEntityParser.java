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

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Match;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class HypertextEntityParser {

  private final Set<ParserEntry<?>> parsers = new HashSet<>();

  public HypertextEntityParser(MatchDataParser matchDataParser) {
    // register autowired parsers
    parsers.add(new ParserEntry<>(Match.class, matchDataParser));
  }

  public <T> Stream<? extends T> getEntityStream(
      @NotNull DataSource<T> dataSource, @NotNull String data) {
    return getParserForType(dataSource.getClazz()).getEntityStream(dataSource, data);
  }

  @SuppressWarnings("unchecked cast")
  private <T> DataSourceParser<T, String> getParserForType(Class<T> clazz) {

    final String errMsg = "No entity parser registered for type: " + clazz;

    return (DataSourceParser<T, String>)
        this.parsers.stream()
            .filter(entry -> entry.getClazz().equals(clazz))
            .findFirst()
            .map(ParserEntry::getParser)
            .orElseThrow(() -> new IllegalArgumentException(errMsg));
  }

  private static class ParserEntry<T> {

    private final Class<T> clazz;
    private final DataSourceParser<T, String> parser;

    public ParserEntry(Class<T> clazz, DataSourceParser<T, String> parser) {
      this.clazz = clazz;
      this.parser = parser;
    }

    public DataSourceParser<T, String> getParser() {
      return parser;
    }

    public Class<T> getClazz() {
      return clazz;
    }
  }
}
