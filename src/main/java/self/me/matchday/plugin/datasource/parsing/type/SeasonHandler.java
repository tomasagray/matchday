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

package self.me.matchday.plugin.datasource.parsing.type;

import org.springframework.stereotype.Component;
import self.me.matchday.model.Season;
import self.me.matchday.plugin.datasource.parsing.TypeHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SeasonHandler extends TypeHandler<Season> {

  public SeasonHandler() {
    super(
        Season.class,
        s -> {
          final Pattern pattern = Pattern.compile("(\\d{2})/(\\d{2})");
          final Matcher matcher = pattern.matcher(s);
          if (matcher.find()) {
            int startYear = Integer.parseInt(matcher.group(1));
            int endYear = Integer.parseInt(matcher.group(2));
            startYear += 2000;
            endYear += 2000;
            return new Season(startYear, endYear);
          }
          return new Season();
        });
  }
}
