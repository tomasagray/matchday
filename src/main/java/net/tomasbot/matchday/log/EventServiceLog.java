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
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import net.tomasbot.matchday.api.service.EventService;
import net.tomasbot.matchday.model.Event;

@Aspect
public class EventServiceLog {

  private static final Logger logger = LogManager.getLogger(EventService.class);

  @Around("execution(* net.tomasbot.matchday.api.service.EventService.save(..))")
  public Object logSaveEvent(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Event event = (Event) jp.getArgs()[0];
    logger.info("Saving Event: {}", event);
    try {
      return jp.proceed();
    } catch (Throwable e) {
      logger.error("Event: {} was not saved to DB; {}", event, e.getMessage());
      throw e;
    }
  }

  @Before("execution(* net.tomasbot.matchday.api.service.EventService.saveAll(..))")
  public void logSaveManyEvents(@NotNull JoinPoint jp) {
    final Iterable<?> events = (Iterable<?>) jp.getArgs()[0];
    final List<?> eventList = StreamSupport.stream(events.spliterator(), false).toList();
    logger.info("Saving: {} Events...", eventList.size());
  }

  @Before("execution(* net.tomasbot.matchday.api.service.EventService.fetchAll(..))")
  public void logFetchAllEvents() {
    logger.info("Fetching ALL Events...");
  }

  @Before("execution(* net.tomasbot.matchday.api.service.EventService.fetchAllPaged(..))")
  public void logFetchLatestEvents(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Fetching Events page #: {} with up to: {} entries", args[0], args[1]);
  }

  @Around("execution(* net.tomasbot.matchday.api.service.EventService.fetchById(..))")
  public Object logGetEventById(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Fetching Event with ID: {} from database...", jp.getArgs()[0]);
    Object result = jp.proceed();
    logger.info("... Retrieved Event: {}", result);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.EventService.fetchEventsForCompetition(..))")
  public Object logFetchEventsForCompetition(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object competitionId = jp.getArgs()[0];
    logger.info("Fetching Events for Competition: {}", competitionId);
    Page<?> events = (Page<?>) jp.proceed();
    logger.info(
        "Retrieved: {} Events for Competition: {}", events.getContent().size(), competitionId);
    return events;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.EventService.update(..))")
  public void logUpdateEvent(@NotNull JoinPoint jp) {
    logger.info("Attempting to update Event: {}", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.EventService.updateAll(..))")
  public void logUpdateManyEvents(@NotNull JoinPoint jp) {
    final Iterable<?> events = (Iterable<?>) jp.getArgs()[0];
    final List<?> eventsList = StreamSupport.stream(events.spliterator(), false).toList();
    logger.info("Updating: {} Events...", eventsList.size());
  }

  @Before("execution(* net.tomasbot.matchday.api.service.EventService.delete(..))")
  public void logDeleteEvent(@NotNull JoinPoint jp) {
    logger.info("Deleting Event: {}", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.EventService.deleteAll(..))")
  public void logDeleteManyEvents(@NotNull JoinPoint jp) {
    final Iterable<?> events = (Iterable<?>) jp.getArgs()[0];
    final List<?> eventList = StreamSupport.stream(events.spliterator(), false).toList();
    logger.info("Deleting: {} Events...", eventList.size());
  }
}
