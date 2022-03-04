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

package self.me.matchday.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class EventDataParserLogging {

  static final Logger logger = LogManager.getLogger(EventDataParserLogging.class);
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Around(
      "execution(*  self.me.matchday.plugin.datasource.parsing.EventDataParser.getEntityStream(..))")
  public Object logAroundEventParsing(@NotNull ProceedingJoinPoint jp) throws Throwable {

    final Object[] args = jp.getArgs();
    logger.info("Argument count: " + args.length);
    logger.info("Arguments: {}\n\n", args[0]);
    logger.info("{}\n\n\n", JsonParser.toJson(args[1]));
    return jp.proceed();
  }
}
