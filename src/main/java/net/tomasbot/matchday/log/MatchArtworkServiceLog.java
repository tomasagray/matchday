package net.tomasbot.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.service.MatchArtworkService;

@Aspect
public class MatchArtworkServiceLog {

  private static final Logger logger = LogManager.getLogger(MatchArtworkService.class);

  @Around("execution(* net.tomasbot.matchday.api.service.MatchArtworkService.makeMatchArtwork(..))")
  public Object logMakeMatchArtwork(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Making new Match artwork for: {}", jp.getArgs()[0]);
    Object result = jp.proceed();
    logger.info("Created new Match artwork: {}", result);
    return result;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.MatchArtworkService.readArtworkData(..))")
  public void logReadArtworkData(@NotNull JoinPoint jp) {
    logger.info("Reading image data for: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.MatchArtworkService.deleteArtworkFromDisk(..))")
  public void logDeleteArtworkFromDisk(@NotNull JoinPoint jp) {
    logger.info("Deleting image data from disk for: {}", jp.getArgs()[0]);
  }
}
