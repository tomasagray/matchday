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

package net.tomasbot.matchday.log.plugin.blogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.plugin.datasource.blogger.BloggerPlugin;

@Aspect
public class BloggerPluginLog {

  private static final Logger logger = LogManager.getLogger(BloggerPlugin.class);

  @Around(
      "execution(* net.tomasbot.matchday.plugin.datasource.blogger.BloggerPlugin.getSnapshot(..))")
  public Object logGetSnapshot(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object[] args = jp.getArgs();
    if (args.length == 2) {
      final Object request = args[0];
      final DataSource<?> dataSource = (DataSource<?>) args[1];
      logger.info(
          "Getting Snapshot using Request: {}, Data Source: {}",
          request,
          dataSource.getDataSourceId());
    }
    return jp.proceed();
  }

  @Before(
      "execution(* net.tomasbot.matchday.plugin.datasource.blogger.BloggerParser.getBlogger(..))")
  public void logGetBloggerFromUrl(@NotNull JoinPoint jp) {
    logger.info("Fetching Blogger instance from: {}", jp.getArgs()[0]);
  }
}
