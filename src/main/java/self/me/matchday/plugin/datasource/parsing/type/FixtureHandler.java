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
import self.me.matchday.model.Fixture;
import self.me.matchday.plugin.datasource.parsing.TypeHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FixtureHandler extends TypeHandler<Fixture> {

    private static final Pattern FIXTURE_NUM = Pattern.compile("^\\d{1,2}$");
    private static final Pattern TOURNAMENT =
            Pattern.compile(
                    "^(Round[\\s-]*of[\\s-]*16)|(Quarter[\\s-]*Final)|" +
                            "(Semi[\\s-]*Final)|(Final)|(Play[\\s-]*off)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_FIXTURE_NUM = Pattern.compile("^(\\w+) (\\d{1,2})$");

    public FixtureHandler() {
        super(
            Fixture.class,
            s -> {
                // fixture number only (e.g., "16")
                if (FIXTURE_NUM.matcher(s).find()) {
                    return new Fixture(Integer.parseInt(s));
                }
                // tournament (e.g., Round of 16, Quarter final, etc.)
                if (TOURNAMENT.matcher(s).find()) {
                    return new Fixture(s);
                }
                // title & number (e.g., Matchday 26, jornada 3, etc.)
                Matcher titleMatcher = TITLE_FIXTURE_NUM.matcher(s);
                if (titleMatcher.find()) {
                    String title = titleMatcher.group(1);
                    int fixture = Integer.parseInt(titleMatcher.group(2));
                    return new Fixture(title, fixture);
                }
                // default
                throw new RuntimeException("Not a Fixture: " + s);
            });
    }
}
