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
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.model.FileServerUser;

import java.util.List;
import java.util.UUID;

@Aspect
public class FileServerUserServiceLog {

  private static final Logger logger = LogManager.getLogger(FileServerUserService.class);

  @Around("execution(* self.me.matchday.api.service.FileServerUserService.login(..))")
  public Object logLoginUser(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FileServerUser user = (FileServerUser) jp.getArgs()[0];
    UUID pluginId = (UUID) jp.getArgs()[1];
    logger.info("Attempting to login User: {} to FileServerPlugin: {}", user, pluginId);
    ClientResponse response = (ClientResponse) jp.proceed();
    if (response.statusCode().is2xxSuccessful()) {
      logger.info("Login SUCCESSFUL for User: {} to FileServerPlugin: {}", user, pluginId);
    } else {
      logger.error("Login FAILED for User: {} to FileServerPlugin: {}", user, pluginId);
    }
    return response;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerUserService.loginWithCookies(..))")
  public Object logLoginUserWithCookies(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID pluginId = (UUID) jp.getArgs()[0];
    FileServerUser user = (FileServerUser) jp.getArgs()[1];
    logger.info(
        "Attempting to login User: {} to FileServerPlugin: {} using cookies", user, pluginId);
    ClientResponse response = (ClientResponse) jp.proceed();
    if (response.statusCode().is2xxSuccessful()) {
      logger.info("Login SUCCESSFUL for User: {} to FileServerPlugin: {}", user, pluginId);
    } else {
      logger.error("Login FAILED for User: {} to FileServerPlugin: {}", user, pluginId);
    }
    return response;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerUserService.logout(..))")
  public Object logLogoutUser(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FileServerUser user = (FileServerUser) jp.getArgs()[0];
    UUID pluginId = (UUID) jp.getArgs()[1];
    logger.info("Attempting to LOGOUT User: {} from FileServerPlugin: {}", user, pluginId);
    ClientResponse response = (ClientResponse) jp.proceed();
    if (response.statusCode().is2xxSuccessful()) {
      logger.info("Successfully logged out User: {} from FileServerPlugin: {}", user, pluginId);
    } else {
      logger.error("Could not log out User: {} from FileServerPlugin: {}", user, pluginId);
    }
    return response;
  }

  @Before("execution(* self.me.matchday.api.service.FileServerUserService.relogin(..))")
  public void logReloginUser(@NotNull JoinPoint jp) {
    Object[] args = jp.getArgs();
    logger.info("Attempting to RE-LOGIN User: {} to FileServerPlugin: {}", args[0], args[1]);
  }

  @SuppressWarnings("unchecked cast")
  @Around("execution(* self.me.matchday.api.service.FileServerUserService.getAllServerUsers(..))")
  public Object logGetAllUsersForPlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Getting all Users for FileServerPlugin: {}", arg);
    List<FileServerUser> users = (List<FileServerUser>) jp.proceed();
    logger.info("Found: {} Users for FileServerPlugin: {}", users.size(), arg);
    return users;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerUserService.getUserById(..))")
  public Object logGetFileServerUser(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Retrieving User with ID: {} from database...", arg);
    Object result = jp.proceed();
    logger.info("Found FileServerUser: {} using ID: {}", result, arg);
    return result;
  }

  @Before("execution(* self.me.matchday.api.service.FileServerUserService.deleteUser(..))")
  public void logDeleteUser(@NotNull JoinPoint jp) {
    logger.info("Attempting to DELETE FileServerUser with ID: {}", jp.getArgs()[0]);
  }
}
