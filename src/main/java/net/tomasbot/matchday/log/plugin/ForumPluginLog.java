package net.tomasbot.matchday.log.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.plugin.datasource.forum.ForumPlugin;

@Aspect
public class ForumPluginLog {

  private static final Logger logger = LogManager.getLogger(ForumPlugin.class);

  @Around("execution(* net.tomasbot.matchday.plugin.datasource.forum.ForumPlugin.readUrlData(..))")
  public Object logReadFromUrl(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Fetching Event data from: {}", jp.getArgs()[0]);
    return jp.proceed();
  }

  @Around("execution(* net.tomasbot.matchday.plugin.datasource.forum.ForumPlugin.getSnapshot(..))")
  public Object logGetSnapshot(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object[] args = jp.getArgs();
    logger.info(
        "Getting Snapshot from DataSource: {} using SnapshotRequest: {}",
        ((DataSource<?>) args[1]).getDataSourceId(),
        args[0]);
    return jp.proceed();
  }

  @Around(
      "execution(* net.tomasbot.matchday.plugin.datasource.forum.ForumPlugin.getUrlSnapshot(..))")
  public Object logGetUrlSnapshot(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object[] args = jp.getArgs();
    logger.info(
        "Getting Snapshot from URL: {} using DataSource: {}",
        args[0],
        ((DataSource<?>) args[1]).getDataSourceId());
    return jp.proceed();
  }

  @Around(
      "execution(* net.tomasbot.matchday.plugin.datasource.forum.ForumPlugin.validateDataSource(..))")
  public Object logValidateDataSource(@NotNull ProceedingJoinPoint jp) throws Throwable {
    DataSource<?> dataSource = (DataSource<?>) jp.getArgs()[0];
    logger.info("Attempting to validate DataSource: {}", dataSource.getDataSourceId());
    return jp.proceed();
  }
}
