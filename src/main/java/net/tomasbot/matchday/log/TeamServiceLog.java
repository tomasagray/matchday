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
import org.springframework.data.domain.Page;
import net.tomasbot.matchday.api.service.TeamService;

@Aspect
public class TeamServiceLog {

  private static final Logger logger = LogManager.getLogger(TeamService.class);

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.fetchAll(..))")
  public void logFetchAllTeams() {
    logger.info("Fetching ALL Teams from database...");
  }

  @Around("execution(* net.tomasbot.matchday.api.service.TeamService.fetchAllPaged(..))")
  public Object logFetchAllTeams(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Fetching Teams from database...");
    Page<?> teams = (Page<?>) jp.proceed();
    logger.info("Found: {} Teams", teams.getContent().size());
    return teams;
  }

  @Around("execution(* net.tomasbot.matchday.api.service.TeamService.fetchById(..))")
  public Object logFetchTeamById(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object teamId = jp.getArgs()[0];
    logger.info("Fetching Team with ID: {} from database...", teamId);
    Object result = jp.proceed();
    logger.debug("... Found Team: {} for query ID: {}", result, teamId);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.TeamService.fetchTeamsByCompetitionId(..))")
  public Object logFetchTeamsByCompetitionId(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object competitionId = jp.getArgs()[0];
    logger.info("Getting Teams for Competition: {}", competitionId);
    List<?> teams = (List<?>) jp.proceed();
    logger.info("Found: {} Teams for Competition: {}", teams.size(), competitionId);
    return teams;
  }

  @Around("execution(* net.tomasbot.matchday.api.service.TeamService.getTeamByName(..))")
  public Object logFetchTeamByName(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object name = jp.getArgs()[0];
    logger.info("Fetching Team with name: {}", name);
    Object result = jp.proceed();
    logger.info("Found Team: {} for name: {}", result, name);
    return result;
  }

  @Around("execution(* net.tomasbot.matchday.api.service.TeamService.save(..))")
  public Object logSaveTeam(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object team = jp.getArgs()[0];
    logger.info("Attempting to save Team: {} to database...", team);
    Object result = jp.proceed();
    if (result != null) {
      logger.info("Team: {} saved to database...", result);
    } else {
      logger.error("Team data: {} could not be saved to database (invalid)", team);
    }
    return result;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.delete(..))")
  public void logDeleteTeam(@NotNull JoinPoint jp) {
    logger.info("Deleting Team: {}", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.deleteAll(..))")
  public void logDeleteManyTeam(@NotNull JoinPoint jp) {
    final Iterable<?> teams = (Iterable<?>) jp.getArgs()[0];
    final long teamCount = teams.spliterator().estimateSize();
    logger.info("Deleting: {} teams...", teamCount);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.deleteTeamByName(..))")
  public void logDeleteTeamByName(@NotNull JoinPoint jp) {
    logger.info("Attempting to DELETE Team: {} from database...", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.fetchArtworkCollection(..))")
  public void logFetchTeamArtworkCollection(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Getting {} artwork collection for Team: {} ...", args[1], args[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.fetchSelectedArtwork(..))")
  public void logFetchSelectedArtwork(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Getting selected {} artwork for Team: {}", args[1], args[0]);
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.TeamService.fetchSelectedArtworkMetadata(..))")
  public void logFetchSelectedArtworkMetadata(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Getting selected {} artwork metadata for Team: {}", args[1], args[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.fetchArtworkImageData(..))")
  public void logFetchTeamArtwork(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Getting {} artwork with ID: {} for Team: {}", args[1], args[2], args[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.TeamService.fetchArtworkMetadata(..))")
  public void logFetchTeamArtworkMetadata(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Getting {} artwork metadata with ID: {} for Team: {}", args[1], args[2], args[0]);
  }
}
