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

import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.PatternKit;
import self.me.matchday.plugin.datasource.parsing.MatchDataParser;

@Aspect
@Component
public class MatchDataParserLog {

  static final Logger logger = LogManager.getLogger(MatchDataParser.class);

  @Before(
      "execution(* self.me.matchday.plugin.datasource.parsing.MatchDataParser.getEntityStream(..))")
  public void logAroundEventParsing(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    if (args.length == 2) {
      if (args[0] instanceof final DataSource<?> dataSource
          && args[1] instanceof final String data) {
        logger.trace(
            "Attempting to get Stream<Event> from data:{} ... using DataSource: {}",
            data.substring(0, Math.min(data.length(), 250)),
            dataSource.getDataSourceId());
      }
    }
  }

  @SuppressWarnings("unchecked cast")
  @Around(
      "execution(* self.me.matchday.plugin.datasource.parsing.MatchDataParser.getStreamForType(..))")
  public Object logGetStreamForType(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final List<PatternKit<?>> patternKits = (List<PatternKit<?>>) jp.getArgs()[0];
    final PatternKit<?> firstPatternKit = patternKits.get(0);
    final String type = firstPatternKit.getClazz().getName();
    final Object data = jp.getArgs()[1];
    logger.debug(
        "Attempting to parse text data with {} PatternKits of type: {}", patternKits.size(), type);
    logger.debug("Parsing text data:\n{}", data);

    final Stream<?> result = (Stream<?>) jp.proceed();
    final List<?> collected = result.toList();
    logger.debug("Found {} elements of type: {}", collected.size(), type);
    return collected.stream();
  }

  @Around(
      "execution(* self.me.matchday.plugin.datasource.parsing.MatchDataParser.createUrlStreams(..))")
  public Object logCreateUrlStream(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Stream<?> urls = (Stream<?>) jp.proceed();
    final List<?> collected = urls.toList();
    logger.debug("Found {} URLs...", collected.size());
    return collected.stream();
  }
}
