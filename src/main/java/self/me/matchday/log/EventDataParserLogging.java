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

package self.me.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.plugin.datasource.parsing.MatchDataParser;
import self.me.matchday.util.JsonParser;

@Aspect
@Component
public class EventDataParserLogging {

  static final Logger logger = LogManager.getLogger(MatchDataParser.class);

  @Before(
      "execution(* self.me.matchday.plugin.datasource.parsing.MatchDataParser.getEntityStream(..))")
  public void logAroundEventParsing(@NotNull JoinPoint jp) {

    final Object[] args = jp.getArgs();
    if (args.length == 2) {
      logger.trace(
          "Attempting to get Stream<Event> from data:\n{}\nUsing DataSource:\n{}",
          args[1],
          JsonParser.toJson(args[0]));
    }
  }
}
