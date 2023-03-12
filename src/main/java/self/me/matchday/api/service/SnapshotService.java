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

package self.me.matchday.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.model.Snapshot;

import java.util.stream.Stream;

@Service
public class SnapshotService {

  private static final Logger logger = LogManager.getLogger(SnapshotService.class);

  private final EntityServiceRegistry registry;

  public SnapshotService(EntityServiceRegistry registry) {
    this.registry = registry;
  }

  public <T> void saveSnapshot(@NotNull Snapshot<T> snapshot, @NotNull Class<T> clazz) {

    final EntityService<T, ?> service = registry.getServiceFor(clazz);
    final Stream<T> data = snapshot.getData();
    data.forEach(
        datum -> {
          try {
            // send to appropriate service
            service.save(datum);
          } catch (Throwable e) {
            logger.info("Did not save Entity of type: [{}]: {}",
                    clazz.getName(), e.getMessage());
          }
        });
  }
}
