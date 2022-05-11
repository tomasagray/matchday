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
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Aspect
public class FileServerServiceLog {

  private static final Logger logger = LogManager.getLogger(FileServerService.class);

  @AfterReturning(
      value =
          "execution(* self.me.matchday.api.service.FileServerService.getFileServerPlugins(..))",
      returning = "allPlugins")
  public void logGetAllFileServerPlugins(@NotNull Object allPlugins) {
    logger.info("Found all FileServerPlugins: {}", allPlugins);
  }

  @AfterReturning(
      value = "execution(* self.me.matchday.api.service.FileServerService.getEnabledPlugins(..))",
      returning = "enabledPlugins")
  public void logGetEnabledFileServerPlugins(@NotNull Object enabledPlugins) {
    logger.info("Found ENABLED FileServerPlugins: {}", enabledPlugins);
  }

  @Around("execution(* self.me.matchday.api.service.FileServerService.isPluginEnabled(..))")
  public Object logIsPluginEnabled(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Boolean enabled = (Boolean) jp.proceed();
    logger.info("Is FileServerPlugin: {} enabled? {}", jp.getArgs()[0], enabled);
    return enabled;
  }

  @SuppressWarnings("unchecked cast")
  @Around("execution(* self.me.matchday.api.service.FileServerService.getPluginById(..))")
  public Object logGetPluginById(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID pluginId = (UUID) jp.getArgs()[0];
    logger.info("Attempting to Get FileServerPlugin with ID: {}", pluginId);
    Optional<FileServerPlugin> pluginOptional = (Optional<FileServerPlugin>) jp.proceed();
    pluginOptional.ifPresentOrElse(
        (plugin) -> logger.info("Found FileServerPlugin: {}", plugin),
        () -> logger.info("No FileServerPlugin found for ID: {}", pluginId));
    return pluginOptional;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerService.disablePlugin(..))")
  public Object logDisablePlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID pluginId = (UUID) jp.getArgs()[0];
    logger.info("Attempting to DISABLE FileServerPlugin with ID: {}", pluginId);
    Boolean disabled = (Boolean) jp.proceed();
    logger.info("Successfully DISABLED FileServerPlugin with ID: {} ? {}", pluginId, disabled);
    return disabled;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerService.enablePlugin(..))")
  public Object logEnablePlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID pluginId = (UUID) jp.getArgs()[0];
    logger.info("Attempting to ENABLE FileServerPlugin with ID: {}", pluginId);
    Boolean disabled = (Boolean) jp.proceed();
    logger.info("Successfully ENABLED FileServerPlugin with ID: {} ? {}", pluginId, disabled);
    return disabled;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerService.login(..))")
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

  @Around("execution(* self.me.matchday.api.service.FileServerService.loginWithCookies(..))")
  public Object logLoginUserWithCookies(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FileServerUser user = (FileServerUser) jp.getArgs()[0];
    UUID pluginId = (UUID) jp.getArgs()[1];
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

  @Around("execution(* self.me.matchday.api.service.FileServerService.logout(..))")
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

  @Before("execution(* self.me.matchday.api.service.FileServerService.relogin(..))")
  public void logReloginUser(@NotNull JoinPoint jp) {
    Object[] args = jp.getArgs();
    logger.info("Attempting to RE-LOGIN User: {} to FileServerPlugin: {}", args[0], args[1]);
  }

  @SuppressWarnings("unchecked cast")
  @Around("execution(* self.me.matchday.api.service.FileServerService.getAllServerUsers(..))")
  public Object logGetAllUsersForPlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Getting all Users for FileServerPlugin: {}", arg);
    List<FileServerUser> users = (List<FileServerUser>) jp.proceed();
    logger.info("Found: {} Users for FileServerPlugin: {}", users.size(), arg);
    return users;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerService.getUserById(..))")
  public Object logGetFileServerUser(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Retrieving User with ID: {} from database...", arg);
    Object result = jp.proceed();
    logger.info("Found FileServerUser: {} using ID: {}", result, arg);
    return result;
  }

  @Before("execution(* self.me.matchday.api.service.FileServerService.deleteUser(..))")
  public void logDeleteUser(@NotNull JoinPoint jp) {
    logger.info("Attempting to DELETE FileServerUser with ID: {}", jp.getArgs()[0]);
  }

  @SuppressWarnings("unchecked cast")
  @Around("execution(* self.me.matchday.api.service.FileServerService.getDownloadUrl(..))")
  public Object logGetInternalUrl(@NotNull ProceedingJoinPoint jp) throws Throwable {

    final URL externalUrl = (URL) jp.getArgs()[0];
    logger.info("Attempting to get internal URL for external URL: {}", externalUrl);
    final Optional<URL> result = (Optional<URL>) jp.proceed();
    if (result.isPresent()) {
      logger.info("Successfully updated internal URL to: {}", result.get());
    } else {
      logger.error("Could not update internal URL from external URL: {}", externalUrl);
    }
    return result;
  }

  @Around(
      "execution(* self.me.matchday.api.service.FileServerService.getFileServerRefreshRate(..))")
  public Object logGetFileServerRefreshRate(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    Object result = jp.proceed();
    logger.info("Got refresh rate of: {} for URL: {}", result, arg);
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.FileServerService.getEnabledPluginForUrl(..))")
  public Object logGetEnabledPluginForUrl(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object url = jp.getArgs()[0];
    Object result = jp.proceed();
    logger.info("Found ENABLED plugin: {} for URL: {}", result, url);
    return result;
  }
}
