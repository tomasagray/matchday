package net.tomasbot.matchday.log;

import java.time.Duration;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.service.admin.DatabaseManagementService;

@Aspect
public class DatabaseManagementServiceLog {

  private static final Logger logger = LogManager.getLogger(DatabaseManagementService.class);

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.DatabaseManagementService.createDatabaseDump(..))")
  public Object logCreateDatabaseDump(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Creating database dump...");
    final Instant start = Instant.now();
    Object result = jp.proceed();
    final Instant end = Instant.now();
    final Duration dumpTime = Duration.between(start, end);
    logger.info("Done dumping database to {}; took: {} milliseconds", result, dumpTime.toMillis());
    return result;
  }
}
