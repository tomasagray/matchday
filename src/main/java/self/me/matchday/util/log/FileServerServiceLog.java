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

package self.me.matchday.util.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.FileServerService;

@Aspect
@Component
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
}
