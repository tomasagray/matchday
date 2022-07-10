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

package self.me.matchday.log.plugin.filefox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.fileserver.filefox.LoginParser;

import java.time.Duration;
import java.time.Instant;

@Aspect
public class LoginParserLog {

  private static final Logger logger = LogManager.getLogger(LoginParser.class);

  @Around("execution(* self.me.matchday.plugin.fileserver.filefox.LoginParser.performLogin(..))")
  public Object logPerformLogin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object user = jp.getArgs()[0];
    logger.info("Performing login for user: {}", user);
    final Instant start = Instant.now();
    final Object result = jp.proceed();
    final Instant end = Instant.now();
    logger.info("Login attempt took: {} seconds", Duration.between(start, end).getSeconds());
    logger.debug("Result was: {}", result);
    return result;
  }

  @Around(
      "execution(* self.me.matchday.plugin.fileserver.filefox.LoginParser.evaluateLoginResponse(..))")
  public Object logEvaluateLoginResponse(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Evaluating login response: {}", jp.getArgs()[0]);
    final Object result = jp.proceed();
    logger.info("After evaluation, response is: {}", result);
    return result;
  }

  @Around("execution(* self.me.matchday.plugin.fileserver.filefox.LoginParser.getLoginData(..))")
  public Object logGetLoginData(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.warn("DELETE THIS METHOD! INSECURE!!!");
    logger.debug("Getting login data for user: {}", jp.getArgs()[0]);
    final Object result = jp.proceed();
    logger.debug("Login data is: {}", result);
    return result;
  }
}
