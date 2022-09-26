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
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.controller.CompetitionController;

@Aspect
public class CompetitionControllerLog {

  private static final Logger logger = LogManager.getLogger(CompetitionController.class);

  @Around("execution(* self.me.matchday.api.controller.CompetitionController.handle*(..))")
  public Object logErrorHandling(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object msg = jp.proceed();
    logger.error(msg);
    return msg;
  }
}
