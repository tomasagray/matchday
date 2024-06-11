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

package net.tomasbot.matchday.startup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import net.tomasbot.matchday.api.service.DataSourceService;
import net.tomasbot.matchday.model.SnapshotRequest;

/** Class to refresh database with data from all remote sources on startup. */
@Configuration
public class InitEventData {

  private static final Logger logger = LogManager.getLogger(InitEventData.class);

  @Bean
  CommandLineRunner initialEventDataLoad(
      final DataSourceService dataSourceService, final Environment env) {
    return args -> {
      final String property = env.getProperty("startup-refresh");
      if (property == null) {
        return;
      }

      logger.info(
          "*** Startup refresh CLI option set; performing initial Event data refresh... ***");
      final SnapshotRequest snapshotRequest = SnapshotRequest.builder().build();
      dataSourceService.refreshAllDataSources(snapshotRequest);
    };
  }
}
