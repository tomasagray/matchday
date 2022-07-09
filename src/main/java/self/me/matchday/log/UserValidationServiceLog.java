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
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.UserValidationService;

@Aspect
public class UserValidationServiceLog {

  private static final Logger logger = LogManager.getLogger(UserValidationService.class);

  @Before(
      "execution(* self.me.matchday.api.service.UserValidationService.validateUserForLogin(..))")
  public void logIsValidUserData(@NotNull JoinPoint jp) {
    Object user = jp.getArgs()[0];
    logger.info("Validating User: {}", user);
  }

  @Before(
      "execution(* self.me.matchday.api.service.UserValidationService.validateEmailAddress(..))")
  public void logIsValidEmailAddress(@NotNull JoinPoint jp) {
    Object email = jp.getArgs()[0];
    logger.info("Validating email address: {} ...", email);
  }

  @Before("execution(* self.me.matchday.api.service.UserValidationService.validatePassword(..))")
  public void logIsValidPassword() {
    logger.info("Validating password: ***** ...");
  }
}
