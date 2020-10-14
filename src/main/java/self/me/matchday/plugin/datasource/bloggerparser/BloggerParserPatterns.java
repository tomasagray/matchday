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

package self.me.matchday.plugin.datasource.bloggerparser;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public abstract class BloggerParserPatterns {

    // Event metadata
    protected String competition;
    protected String fixture;
    protected String teams;
    protected String date;
    protected String season;
    protected String filesize;

    public Matcher getCompetitionMatcher(@NotNull final String data) {
      return
          Pattern
              .compile(competition, Pattern.CASE_INSENSITIVE)
              .matcher(data);
    }

    public Matcher getSeasonMatcher(@NotNull final String data) {
      return
          Pattern
              .compile(season, Pattern.CASE_INSENSITIVE)
              .matcher(data);
    }

    public Matcher getFixtureMatcher(@NotNull final String data) {
      return
          Pattern
              .compile(fixture, Pattern.CASE_INSENSITIVE)
              .matcher(data);
    }

    public Matcher getDateMatcher(@NotNull final String data) {
      return
          Pattern
              .compile(date, Pattern.CASE_INSENSITIVE)
              .matcher(data);
    }

    public Matcher getTeamsMatcher(@NotNull final String data) {
      return
          Pattern
              .compile(teams, Pattern.CASE_INSENSITIVE)
              .matcher(data);
    }

    public Matcher getFilesizeMatcher(@NotNull final String data) {
      return
          Pattern
              .compile(filesize, Pattern.CASE_INSENSITIVE)
              .matcher(data);
    }
}
