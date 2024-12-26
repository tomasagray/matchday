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

package net.tomasbot.matchday.plugin.datasource.parsing.type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.tomasbot.matchday.model.Fixture;
import org.springframework.stereotype.Component;

@Component
public class FixtureHandler extends TypeHandler<Fixture> {

  private static final Pattern FIXTURE_NUM = Pattern.compile("^\\d{1,2}$");
  private static final Pattern TITLE_FIXTURE_NUM = Pattern.compile("^(\\w+) (\\d{1,2})$");
  private static final Pattern GROUP_STAGE =
      Pattern.compile("\\bgroup\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern ROUND_ROBIN =
      Pattern.compile("\\b[roundf\\s]+\\b(\\d+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern QUARTER_FINAL =
      Pattern.compile("\\bquarter[final]*\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern SEMI_FINAL =
      Pattern.compile("\\bsemi[final]*\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern FINAL = Pattern.compile("\\bfinal\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern PLAYOFF =
      Pattern.compile("\\bplay-*of+\\b", Pattern.CASE_INSENSITIVE);

  public FixtureHandler() {
    super(
        Fixture.class,
        s -> {
          // fixture number only (e.g., "13")
          if (FIXTURE_NUM.matcher(s).find()) {
            return new Fixture(Integer.parseInt(s));
          }

          // title & number (e.g., Matchday 26, jornada 3, etc.)
          Matcher titleMatcher = TITLE_FIXTURE_NUM.matcher(s);
          if (titleMatcher.find()) {
            int fixture = Integer.parseInt(titleMatcher.group(2));
            String title = String.format("%s %d", titleMatcher.group(1), fixture);
            return new Fixture(title, fixture);
          }

          if (GROUP_STAGE.matcher(s).find()) return new Fixture(Fixture.GroupStage);
          if (QUARTER_FINAL.matcher(s).find()) return new Fixture(Fixture.QuarterFinal);
          if (SEMI_FINAL.matcher(s).find()) return new Fixture(Fixture.SemiFinal);
          if (PLAYOFF.matcher(s).find()) return new Fixture(Fixture.Playoff);
          if (FINAL.matcher(s).find()) return new Fixture(Fixture.Final);

          Matcher roundMatcher = ROUND_ROBIN.matcher(s);
          if (roundMatcher.find()) {
            int roundOf = Integer.parseInt(roundMatcher.group(1));
            return switch (roundOf) {
              case 16 -> new Fixture(Fixture.RoundOf16);
              case 32 -> new Fixture(Fixture.RoundOf32);
              case 64 -> new Fixture(Fixture.RoundOf64);
              default -> new Fixture("Round of " + roundOf, 1_024 + roundOf);
            };
          }

          // default
          throw new RuntimeException("Not a Fixture: " + s);
        });
  }
}
