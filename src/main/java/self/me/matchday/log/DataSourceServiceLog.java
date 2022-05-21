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
import self.me.matchday.api.service.DataSourceService;

import java.util.List;

@Aspect
public class DataSourceServiceLog {

  private static final Logger logger = LogManager.getLogger(DataSourceService.class);

  @Before("execution(* self.me.matchday.api.service.DataSourceService.refreshAllDataSources(..))")
  public void logDataSourceRefresh(@NotNull JoinPoint jp) {
    logger.info("Refreshing all DataSources with SnapshotRequest: {}", jp.getArgs());
  }

  @Before("execution(* self.me.matchday.api.service.DataSourceService.save(..))")
  public void logSaveNewDataSource(@NotNull JoinPoint jp) {
    logger.info("Attempting to save new DataSource: {}", jp.getArgs()[0]);
  }

  @Around("execution(* self.me.matchday.api.service.DataSourceService.getDataSourcesForPlugin(..))")
  public Object logGetDataSourcesForPlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Fetching Data Sources for plugin ID: {}", jp.getArgs()[0]);
    Object result = jp.proceed();
    if (result instanceof List) {
      final List<?> sources = (List<?>) result;
      logger.info("Retrieved {} sources from database", sources.size());
    } else {
      logger.error("Database returned invalid data: {}", result);
    }
    return result;
  }

  @Before("execution(* self.me.matchday.api.service.DataSourceService.getDataSourceById(..))")
  public void logGetDataSourceById(@NotNull JoinPoint jp) {
    logger.info("Getting DataSource with ID: {}", jp.getArgs()[0]);
  }

  @Before("execution(* self.me.matchday.api.service.DataSourceService.delete(..))")
  public void logDeleteDataSource(@NotNull JoinPoint jp) {
    logger.info("Deleting Data Source: {}", jp.getArgs()[0]);
  }
}
