/*
 * Copyright (c) 2023.
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
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Aspect
public class SettingsServiceLog {

  private static final Logger logger = LogManager.getLogger(SettingsServiceLog.class);

  @AfterReturning(
      value = "execution(* self.me.matchday.api.service.SettingsService.getSettings(..))",
      returning = "settings")
  public void logGetSettings(@Nullable Object settings) {
    logger.debug("Application settings are: {}", settings);
  }

  @AfterReturning(
      value = "execution(* self.me.matchday.api.service.SettingsService.updateSettings(..))",
      returning = "settings")
  public void logUpdateSettings(@Nullable Object settings) {
    logger.info("Updated application settings to: {}", settings);
  }

  @Around("execution(* self.me.matchday.api.service.SettingsService.deleteSettingsHistory(..))")
  public Object logDeleteSettingsHistory(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Deleting application settings history before: {}", jp.getArgs()[0]);
    final Object deleted = jp.proceed();
    logger.info("Deleted: {} records", deleted);
    return deleted;
  }
}
