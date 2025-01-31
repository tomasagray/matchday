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

package net.tomasbot.matchday.log;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.service.MatchService;

@Aspect
public class MatchServiceLog {

  private static final Logger logger = LogManager.getLogger(MatchService.class);

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.fetchAll(..))")
  public void logFetchAllMatches() {
    logger.info("Fetching all Matches from database...");
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.fetchAllPaged(..))")
  public void logFetchPagedMatches(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Getting Teams page #: {} with up to: {} entries", args[0], args[1]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.fetchById(..))")
  public void logFetchMatchById(@NotNull JoinPoint jp) {
    logger.info("Fetching Match with ID: {}", jp.getArgs()[0]);
  }

  @Around("execution(* net.tomasbot.matchday.api.service.MatchService.fetchMatchesForTeam(..))")
  public Object logFetchEventsForTeam(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object teamId = jp.getArgs()[0];
    logger.info("Fetching Events for Team: {}", teamId);
    List<?> events = (List<?>) jp.proceed();
    logger.info("Retrieved: {} Events for Team: {}", events.size(), teamId);
    return events;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.refreshMatchArtwork(..))")
  public void logRefreshMatchArtwork(@NotNull JoinPoint jp) {
    logger.info("Refreshing artwork for Match: {}", jp.getArgs()[0]);
  }

  @Around("execution(* net.tomasbot.matchday.api.service.MatchService.save(..))")
  public Object logSaveMatch(@NotNull ProceedingJoinPoint jp) throws Throwable {
    try {
      logger.info("Saving Match: {}", jp.getArgs()[0]);
      final Object saved = jp.proceed();
      logger.info("Match saved as: {}", saved);
      return saved;
    } catch (Throwable e) {
      logger.error("Could not save Match: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.saveAll(..))")
  public void logSaveAllMatches(@NotNull JoinPoint jp) {
    final Iterable<?> matches = (Iterable<?>) jp.getArgs()[0];
    final long matchCount = matches.spliterator().getExactSizeIfKnown();
    logger.info("Saving {} Matches to database...", matchCount);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.update(..))")
  public void logUpdateMatch(@NotNull JoinPoint jp) {
    logger.info("Updating Match: {}...", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.updateAll(..))")
  public void logUpdateSeveralMatches(@NotNull JoinPoint jp) {
    final Iterable<?> matches = (Iterable<?>) jp.getArgs()[0];
    final long matchCount = matches.spliterator().getExactSizeIfKnown();
    logger.info("Updating {} Matches...", matchCount);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.delete(..))")
  public void logDeleteMatch(@NotNull JoinPoint jp) {
    logger.info("DELETING Match: {}", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchService.deleteAll(..))")
  public void logDeleteSeveralMatches(@NotNull JoinPoint jp) {
    final Iterable<?> matches = (Iterable<?>) jp.getArgs()[0];
    final long matchCount = matches.spliterator().getExactSizeIfKnown();
    logger.info("DELETING: {} Matches from database...", matchCount);
  }
}
