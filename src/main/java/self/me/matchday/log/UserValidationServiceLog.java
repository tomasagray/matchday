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
import self.me.matchday.api.service.UserValidationService;

@Aspect
public class UserValidationServiceLog {

  private static final Logger logger = LogManager.getLogger(UserValidationService.class);

  @Around("execution(* self.me.matchday.api.service.UserValidationService.validateUser(..))")
  public Object logIsValidUserData(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object user = jp.getArgs()[0];
    logger.info("Validating User: {}", user);
    Object valid = jp.proceed();
    logger.info("User: {} is valid? {}", user, valid);
    return valid;
  }

  @Around("execution(* self.me.matchday.api.service.UserValidationService.isValidEmailAddress(..))")
  public Object logIsValidEmailAddress(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object email = jp.getArgs()[0];
    logger.info("Validating email address: {} ...", email);
    Object valid = jp.proceed();
    logger.info("Email address: {} is valid? {}", email, valid);
    return valid;
  }

  @Around("execution(* self.me.matchday.api.service.UserValidationService.isValidPassword(..))")
  public Object logIsValidPassword(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Validating password: ***** ...");
    Object result = jp.proceed();
    logger.info("Password is valid? {}", result);
    return result;
  }
}
