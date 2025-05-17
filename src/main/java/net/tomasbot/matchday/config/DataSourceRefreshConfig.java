package net.tomasbot.matchday.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class DataSourceRefreshConfig {

  @Value("${plugin.forum.refresh-threads}")
  private int REFRESH_THREADS;

  @Value("${plugin.forum.max-refresh-threads}")
  private int MAX_REFRESH_THREADS;

  @Value("${plugin.forum.thread-prefix}")
  private String THREAD_PREFIX;

  @Bean(name = "DataSourceRefresher")
  public TaskExecutor getVideoStreamer() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(REFRESH_THREADS);
    executor.setMaxPoolSize(MAX_REFRESH_THREADS);
    executor.setQueueCapacity(Integer.MAX_VALUE);
    executor.setThreadNamePrefix(THREAD_PREFIX);
    executor.initialize();

    return executor;
  }
}
