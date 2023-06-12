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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.model.Competition;

@Aspect
public class CompetitionServiceLog {

  private static final Logger logger = LogManager.getLogger(CompetitionService.class);

  @SuppressWarnings("unchecked cast")
  @Around("execution(* self.me.matchday.api.service.CompetitionService.fetchAll(..))")
  public Object logFetchAllCompetitions(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Retrieving all Competitions from database...");
    List<Competition> competitions = (List<Competition>) jp.proceed();
    logger.info("Database returned: {} Competitions.", competitions.size());
    return competitions;
  }

  @Around("execution(* self.me.matchday.api.service.CompetitionService.fetchCompetitionBy*(..))")
  public Object logFetchCompetitionById(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Fetching competition using: {} from the database...", arg);
    Object result = jp.proceed();
    logger.info("Found competition: {} using: {}", result, arg);
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.CompetitionService.save(..))")
  public Object logSaveCompetition(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Attempting to save Competition: {}", jp.getArgs()[0]);
    Object result = jp.proceed();
    logger.info("Save resulted in: {}", result);
    return result;
  }

  @Before("execution(* self.me.matchday.api.service.CompetitionService.delete(..))")
  public void logDeleteCompetitionById(@NotNull JoinPoint jp) {
    logger.info("Deleting Competition with ID: [{}] from database", jp.getArgs()[0]);
  }
}
