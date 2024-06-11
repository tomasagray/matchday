package net.tomasbot.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.service.admin.BackupService;

@Aspect
public class BackupServiceLog {

  private static final Logger logger = LogManager.getLogger(BackupService.class);

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.BackupService.createRestorePoint(..))")
  public Object logCreateRestorePoint(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Creating System Restore Point...");
    Object result = jp.proceed();
    logger.info("Done creating System Restore Point.");
    return result;
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.admin.BackupService.fetchAllRestorePoints(..))")
  public void logFetchAllRestorePoints() {
    logger.info("Retrieving all System Restore Points from database...");
  }

  @Around("execution(* net.tomasbot.matchday.api.service.admin.BackupService.createBackup(..))")
  public Object logCreateBackup(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Creating system backup archive...");
    Object result = jp.proceed();
    logger.info("Created system backup archive at: {}", result);
    return result;
  }

  @Around("execution(* net.tomasbot.matchday.api.service.admin.BackupService.restoreSystem(..))")
  public Object logRestoreSystem(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object restorePointId = jp.getArgs()[0];
    logger.info("Restoring system from System Restore Point: {}", restorePointId);
    Object result = jp.proceed();
    logger.info("Finished restoring system from System Restore Point: {}", restorePointId);
    return result;
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.admin.BackupService.loadBackupArchive(..))")
  public void logLoadBackupArchive(@NotNull JoinPoint jp) {
    logger.info("Loading system from Backup Archive: {}", jp.getArgs()[0]);
  }

  @Before(
      "execution(* net.tomasbot.matchday.api.service.admin.BackupService.readBackupArchive(..))")
  public void logReadBackupArchiveFromDisk(@NotNull JoinPoint jp) {
    logger.info(
        "Reading System Restore backup archive for System Restore Point: {}", jp.getArgs()[0]);
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.admin.BackupService.deleteRestorePoint(..))")
  public Object logDeleteRestorePoint(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object rpId = jp.getArgs()[0];
    logger.info("Deleting System Restore Point: {}...", rpId);
    Object result = jp.proceed();
    logger.info("Successfully deleted System Restore Point: {}", rpId);
    return result;
  }

  @Around("execution(* net.tomasbot.matchday.api.service.admin.BackupService.dehydrateToDisk(..))")
  public Object logDehydrateToDisk(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Dehydrating system to disk...");
    Object result = jp.proceed();
    logger.info("Finished dehydrating system to: {}", result);
    return result;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.admin.BackupService.dehydrate(..))")
  public void logDehydrateSystem() {
    logger.info("Dehydrating system...");
  }

  @Around("execution(* net.tomasbot.matchday.api.service.admin.BackupService.rehydrateFrom(..))")
  public Object logRehydrateSystemFromDisk(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Rehydrating system from: {}", jp.getArgs()[0]);
    Object result = jp.proceed();
    logger.info("Finished rehydrating system.");
    return result;
  }
}
