package net.tomasbot.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.service.admin.SanityCheckService;

@Aspect
public class SanityCheckServiceLog {

  private static final Logger logger = LogManager.getLogger(SanityCheckService.class);

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.SanityCheckService.createSanityReport(..))")
  public Object logGenerateSanityReport(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Generating system Sanity Report...");
    Object result = jp.proceed();
    logger.info("Finished generating System sanity Report.");
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.SanityCheckService.createArtworkSanityReport(..))")
  public Object logGenerateArtworkReport(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Generating Artwork sanity report...");
    Object result = jp.proceed();
    logger.info("Finished generating Artwork sanity report.");
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.SanityCheckService.createVideoSanityReport(..))")
  public Object logGenerateVideoReport(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Generating Video sanity report...");
    Object result = jp.proceed();
    logger.info("Finished generating Video sanity report.");
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.SanityCheckService.autoHealSystem(..))")
  public Object logAutoHealSystem(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Attempting to auto-heal system...");
    Object result = jp.proceed();
    logger.info("Finished auto-healing system.");
    return result;
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.SanityCheckService.autoHealArtwork(..))")
  public Object logAutoHealArtwork(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Attempting to auto-heal Artwork...");
    Object result = jp.proceed();
    logger.info("Finished auto-healing artwork.");
    return result;
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.admin.SanityCheckService.deleteArtworkFiles(..))")
  public void logDeleteDanglingArtworkFiles() {
    logger.info("Deleting dangling Artwork files...");
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.SanityCheckService.autoHealVideos(..))")
  public Object logAutoHealVideos(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Attempting to auto-heal videos...");
    Object result = jp.proceed();
    logger.info("Finished auto-healing videos.");
    return result;
  }
}
