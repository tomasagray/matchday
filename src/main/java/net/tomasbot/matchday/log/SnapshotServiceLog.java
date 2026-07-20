package net.tomasbot.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;

@Aspect
public class SnapshotServiceLog {

  private static final Logger logger = LogManager.getLogger(SnapshotServiceLog.class);

  @Before("execution(* net.tomasbot.matchday.api.service.SnapshotService.saveSnapshot(..))")
  public void logSaveSnapshot(@NotNull JoinPoint jp) {
    final Object snapshot = jp.getArgs()[0];
    logger.info("Saving snapshot: {}", snapshot);
  }
}
