package net.tomasbot.matchday.log;

import java.time.Duration;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;

@Aspect
public class FlaresolverrServiceLog {

  private static final Logger logger = LogManager.getLogger(FlaresolverrServiceLog.class);

  @Around(
      "execution(* net.tomasbot.matchday.common.flaresolverr.FlaresolverrService.solveChallengeFor(..) )")
  public Object logSolveFlareChallenge(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    logger.info("Submitting URL: {} to Flaresolverr...", jp.getArgs());
    Object result = jp.proceed();

    Instant end = Instant.now();
    logger.info("Flaresolverr took: {}ms", Duration.between(start, end).toMillis());
    logger.debug("Flaresolverr Solution: {}", result);
    return result;
  }
}
