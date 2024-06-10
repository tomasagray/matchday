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
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.FileServerUserService;
import self.me.matchday.model.FileServerUser;

@Aspect
public class FileServerUserServiceLog {

  private static final Logger logger = LogManager.getLogger(FileServerUserService.class);

  @Around("execution(* self.me.matchday.api.service.FileServerUserService.login(..))")
  public Object logLoginUser(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FileServerUser user = (FileServerUser) jp.getArgs()[0];
    logger.info("Attempting to login User: {} to FileServerPlugin: {}", user, user.getServerId());
    FileServerUser loggedInUser = (FileServerUser) jp.proceed();
    logger.info(
        "User: {} logged in successfully? {}",
        loggedInUser.getUsername(),
        loggedInUser.isLoggedIn());
    return loggedInUser;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerUserService.loginWithCookies(..))")
  public Object logLoginUserWithCookies(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FileServerUser user = (FileServerUser) jp.getArgs()[0];
    logger.info(
        "Attempting to login User: {} to FileServerPlugin: {} using cookies",
        user,
        user.getServerId());
    FileServerUser loggedInUser = (FileServerUser) jp.proceed();
    logger.info(
        "User: {} logged in with cookies successfully? {}", user.getUsername(), user.isLoggedIn());
    return loggedInUser;
  }

  @Before(
      "execution(* self.me.matchday.api.service.FileServerUserService.setUserLoggedInToServer(..))")
  public void logSetUserLoggedInToServer(@NotNull JoinPoint jp) {
    final FileServerUser user = (FileServerUser) jp.getArgs()[0];
    final UUID serverId = (UUID) jp.getArgs()[1];
    logger.info(
        "Determining if user: {} already exists for server: {}", user.getUsername(), serverId);
  }

  @Around("execution(* self.me.matchday.api.service.FileServerUserService.logout(..))")
  public Object logLogoutUser(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID userId = (UUID) jp.getArgs()[0];
    logger.info("Attempting to LOGOUT User: {} ", userId);
    FileServerUser loggedOutUser = (FileServerUser) jp.proceed();
    logger.info(
        "User: {} logged out successfully? {}",
        loggedOutUser.getUsername(),
        !loggedOutUser.isLoggedIn());
    return loggedOutUser;
  }

  @Before("execution(* self.me.matchday.api.service.FileServerUserService.relogin(..))")
  public void logReloginUser(@NotNull JoinPoint jp) {
    logger.info("Attempting to RE-LOGIN User: {}", jp.getArgs()[0]);
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
