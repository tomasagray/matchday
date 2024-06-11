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

import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.service.FileServerPluginService;
import net.tomasbot.matchday.plugin.fileserver.FileServerPlugin;

@Aspect
public class FileServerPluginServiceLog {

  private static final Logger logger = LogManager.getLogger(FileServerPluginService.class);

  @AfterReturning(
      value =
          "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.getFileServerPlugins(..))",
      returning = "allPlugins")
  public void logGetAllFileServerPlugins(@NotNull Object allPlugins) {
    logger.info("Found all FileServerPlugins: {}", allPlugins);
  }

  @AfterReturning(
      value =
          "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.getEnabledPlugins(..))",
      returning = "enabledPlugins")
  public void logGetEnabledFileServerPlugins(@NotNull Object enabledPlugins) {
    logger.info("Found ENABLED FileServerPlugins: {}", enabledPlugins);
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.isPluginEnabled(..))")
  public Object logIsPluginEnabled(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Boolean enabled = (Boolean) jp.proceed();
    logger.info("Is FileServerPlugin: {} enabled? {}", jp.getArgs()[0], enabled);
    return enabled;
  }

  @SuppressWarnings("unchecked cast")
  @Around(
      "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.getPluginById(..))")
  public Object logGetPluginById(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID pluginId = (UUID) jp.getArgs()[0];
    logger.info("Attempting to Get FileServerPlugin with ID: {}", pluginId);
    Optional<FileServerPlugin> pluginOptional = (Optional<FileServerPlugin>) jp.proceed();
    pluginOptional.ifPresentOrElse(
        (plugin) -> logger.info("Found FileServerPlugin: {}", plugin),
        () -> logger.info("No FileServerPlugin found for ID: {}", pluginId));
    return pluginOptional;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.disablePlugin(..))")
  public Object logDisablePlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID pluginId = (UUID) jp.getArgs()[0];
    logger.info("Attempting to DISABLE FileServerPlugin with ID: {}", pluginId);
    Boolean disabled = (Boolean) jp.proceed();
    logger.info("Successfully DISABLED FileServerPlugin with ID: {} ? {}", pluginId, disabled);
    return disabled;
  }

  @Around("execution(* net.tomasbot.matchday.api.service.FileServerPluginService.enablePlugin(..))")
  public Object logEnablePlugin(@NotNull ProceedingJoinPoint jp) throws Throwable {
    UUID pluginId = (UUID) jp.getArgs()[0];
    logger.info("Attempting to ENABLE FileServerPlugin with ID: {}", pluginId);
    Boolean disabled = (Boolean) jp.proceed();
    logger.info("Successfully ENABLED FileServerPlugin with ID: {} ? {}", pluginId, disabled);
    return disabled;
  }

  @SuppressWarnings("unchecked cast")
  @Around(
      "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.getDownloadUrl(..))")
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
      "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.getFileServerRefreshRate(..))")
  public Object logGetFileServerRefreshRate(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    Object result = jp.proceed();
    logger.info("Got refresh rate of: {} for URL: {}", result, arg);
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.FileServerPluginService.getEnabledPluginForUrl(..))")
  public Object logGetEnabledPluginForUrl(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object url = jp.getArgs()[0];
    Object result = jp.proceed();
    logger.info("Found ENABLED plugin: {} for URL: {}", result, url);
    return result;
  }
}
