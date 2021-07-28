/*
 * Copyright (c) 2021.
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

package self.me.matchday.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class VideoStreamingConfig implements AsyncConfigurer {

  // todo - move these to resource file
  private static final int THREAD_COUNT = 4;
  private static final int QUEUE_SIZE = 50;
  private static final int MAX_POOL_SIZE = 12;
  private static final String THREAD_NAME_PREFIX = "Matchday-VideoStreamJob-";

  @Override
  public Executor getAsyncExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(THREAD_COUNT);
    executor.setMaxPoolSize(MAX_POOL_SIZE);
    executor.setQueueCapacity(QUEUE_SIZE);
    executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
    executor.initialize();
    return executor;
  }
}
