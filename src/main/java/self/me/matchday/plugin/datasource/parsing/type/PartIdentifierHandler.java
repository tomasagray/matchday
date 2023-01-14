/*
 * Copyright (c) 2023.
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

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.plugin.datasource.parsing.TypeHandler;

@Component
public class PartIdentifierHandler extends TypeHandler<PartIdentifier> {

  private static final Map<Pattern, PartIdentifier> patterns =
      Map.of(
          Pattern.compile("Pre"),
          PartIdentifier.PRE_MATCH,
          Pattern.compile("1st|First"),
          PartIdentifier.FIRST_HALF,
          Pattern.compile("2nd|Second"),
          PartIdentifier.SECOND_HALF,
          Pattern.compile("Extra"),
          PartIdentifier.EXTRA_TIME,
          Pattern.compile("Trophy"),
          PartIdentifier.TROPHY_CEREMONY,
          Pattern.compile("Post"),
          PartIdentifier.POST_MATCH);

  public PartIdentifierHandler() {
    super(
        PartIdentifier.class,
        s ->
            patterns.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(s).find())
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(PartIdentifier.DEFAULT));
  }
}
