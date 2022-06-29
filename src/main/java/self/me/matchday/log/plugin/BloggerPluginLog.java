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

package self.me.matchday.log.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.BloggerPlugin;

@Aspect
public class BloggerPluginLog {

  private static final Logger logger = LogManager.getLogger(BloggerPlugin.class);

  @Around("execution(* self.me.matchday.plugin.datasource.blogger.BloggerPlugin.getSnapshot(..))")
  public Object logGetSnapshot(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object[] args = jp.getArgs();
    logger.info("Getting Snapshot using Request: {}", args[0]);
    logger.debug("Getting Snapshot using Request: {}, Data Source: {}", args[0], args[1]);
    return jp.proceed();
  }

  @Around("execution(* self.me.matchday.plugin.datasource.blogger.BloggerPlugin.isEnabled())")
  public Object logIsBloggerPluginEnabled(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object enabled = jp.proceed();
    logger.info("Is Blogger plugin currently enabled? {}", enabled);
    return enabled;
  }

  @Before("execution(* self.me.matchday.plugin.datasource.blogger.BloggerPlugin.setEnabled(..))")
  public void logSetBloggerPluginEnabled(@NotNull JoinPoint jp) {
    logger.info("Setting Blogger plugin enabled status to: {}", jp.getArgs()[0]);
  }
}
