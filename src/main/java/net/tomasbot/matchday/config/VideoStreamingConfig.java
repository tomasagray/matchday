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

package net.tomasbot.matchday.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class VideoStreamingConfig {

  @Value("${video-resources.simultaneous-streams}")
  private int SIMULTANEOUS_STREAMS;

  @Value("${video-resources.max-pool-size}")
  private int MAX_POOL_SIZE;

  @Value("${video-resources.queue-size}")
  private int QUEUE_SIZE;

  @Value("${video-resources.thread-name-prefix}")
  private String STREAM_THREAD_PREFIX;

  @Value("${video-resources.simultaneous-refresh}")
  private int MAX_REFRESH;

  @Value("${video-resources.refresh-task-prefix}")
  private String REFRESH_THREAD_PREFIX;

  @Bean(name = "VideoStreamExecutor")
  public TaskExecutor getVideoStreamer() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(SIMULTANEOUS_STREAMS);
    executor.setMaxPoolSize(MAX_POOL_SIZE);
    executor.setQueueCapacity(QUEUE_SIZE);
    executor.setThreadNamePrefix(STREAM_THREAD_PREFIX);
    executor.initialize();
    return executor;
  }

  @Bean(name = "VideoFileRefresher")
  public Executor getVideoFileRefresher() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(MAX_REFRESH);
    executor.setThreadNamePrefix(REFRESH_THREAD_PREFIX);
    executor.initialize();
    return executor;
  }
}
