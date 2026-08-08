package net.tomasbot.matchday.log.plugin;

import java.net.URI;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.plugin.datasource.forum.EventMetaDataRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;

@Aspect
public class ForumPluginLog {

  private static final Logger logger = LogManager.getLogger(ForumPluginLog.class);

  @Before(
      "execution(* net.tomasbot.matchday.plugin.datasource.forum.RemoteDataReader.readDataFrom(..))")
  public void logReadFromUrl(@NotNull JoinPoint jp) {
    logger.info("Fetching Event data from: {}", jp.getArgs()[0]);
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

  @Before(
      "execution(* net.tomasbot.matchday.plugin.datasource.forum.FileSourceCorrectorMutator.correctFileSource(..))")
  public void logFixFileSource(@NotNull JoinPoint jp) {
    logger.info("Repairing broken/missing data in File Source: {}", jp.getArgs());
  }

  @Before(
      "execution(* net.tomasbot.matchday.plugin.datasource.forum.EventReader.readListEvent(..))")
  public void logReadEventMetadata(@NotNull JoinPoint jp) {
    Object[] args = jp.getArgs();
    if (args.length == 0) return;

    if (args.length == 1 && args[0] instanceof EventMetaDataRequest request) {
      URI uri = request.getUri();
      logger.info("Reading Event metadata from: {}", uri);
    }
  }

  @Before(
      "execution(* net.tomasbot.matchday.plugin.datasource.forum.EventReader.handleReadMetadataError(..))")
  public void logHandleReadMetadataError(@NotNull JoinPoint jp) {
    Object[] args = jp.getArgs();
    if (args.length == 0) return;

    if (args.length == 2 && args[0] instanceof Throwable e) {
      Object uri = args[1];
      logger.error("Error reading Event metadata from: {}", uri, e);
    }
  }

  @Before(
      "execution(* net.tomasbot.matchday.plugin.datasource.forum.EventReader.handleReadEventError(..))")
  public void logReadEventError(@NotNull JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (args.length == 0) return;

    if (args.length == 2 && args[0] instanceof Throwable e) {
      Object url = args[1];
      logger.error("Error reading Event from: {}", url, e);
    }
  }
}
