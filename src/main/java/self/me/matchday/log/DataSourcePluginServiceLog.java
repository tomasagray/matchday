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
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.DataSourcePluginService;

import java.util.Collection;

@Aspect
public class DataSourcePluginServiceLog {

  private static final Logger logger = LogManager.getLogger(DataSourcePluginService.class);

  @Around(
      "execution(* self.me.matchday.api.service.DataSourcePluginService.getDataSourcePlugins())")
  public Object logGetAllDataSourcePlugins(@NotNull ProceedingJoinPoint jp) throws Throwable {

    logger.info("Retrieving all DataSourcePlugins from database...");
    Object result = jp.proceed();
    logger.info("Found {} DataSourcePlugins", countPlugins(result));
    return result;
  }

  private int countPlugins(Object result) {
    int count = 0;
    if (result instanceof Collection) {
      count = ((Collection<?>) result).size();
    }
    return count;
  }

  @Around("execution(* self.me.matchday.api.service.DataSourcePluginService.getEnabledPlugins())")
  public Object logGetEnabledDataSourcePlugins(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Looking up all ENABLED DataSource plugins");
    final Object result = jp.proceed();
    final int pluginCount = countPlugins(result);
    logger.info("Found: {} ENABLED DataSource plugins.", pluginCount);
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.DataSourcePluginService.enablePlugin(..))")
  public Object logEnableDataSourcePlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Attempting to enable DataSourcePlugin: {}", arg);
    Object result = jp.proceed();
    logger.info("Successfully ENABLED DataSourcePlugin: {}", arg);
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.DataSourcePluginService.disablePlugin(..))")
  public Object logDisableDataSourcePlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Attempting to disable DataSourcePlugin: {}", arg);
    Object result = jp.proceed();
    logger.info("Successfully DISABLED DataSourcePlugin: {}", arg);
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.DataSourcePluginService.isPluginEnabled(..))")
  public Object logIsPluginEnabled(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object pluginId = jp.getArgs()[0];
    final Object enabled = jp.proceed();
    logger.info("Is DataSourcePlugin: {} enabled? {}", pluginId, enabled);
    return enabled;
  }

  @Before(
      "execution(* self.me.matchday.api.service.DataSourcePluginService.validateDataSource(..))")
  public void logValidateDataSource(@NotNull JoinPoint jp) {
    logger.info("Validating DataSource: {}", jp.getArgs()[0]);
  }
}
