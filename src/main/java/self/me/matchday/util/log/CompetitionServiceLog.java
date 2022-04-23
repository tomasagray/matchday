package self.me.matchday.util.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.CompetitionService;

import java.util.Optional;

@Aspect
public class CompetitionServiceLog {

  private static final Logger logger = LogManager.getLogger(CompetitionService.class);

  @Around("execution(* self.me.matchday.api.service.CompetitionService.fetchAllCompetitions(..))")
  public Object logFetchAllCompetitions(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Retrieving all Competitions from database...");
    Object result = jp.proceed();
    if (result != null) {
      final Optional<?> opt = (Optional<?>) result;
      if (opt.isEmpty()) {
        logger.info("Attempted to fetch all Competitions, but none returned");
      }
    }
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.CompetitionService.fetchCompetitionBy*(..))")
  public Object logFetchCompetitionById(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object arg = jp.getArgs()[0];
    logger.info("Fetching competition using: {} from the database...", arg);
    Object result = jp.proceed();
    logger.info("Found competition: {} using: {}", result, arg);
    return result;
  }

  @Around("execution(* self.me.matchday.api.service.CompetitionService.saveCompetition(..))")
  public Object logSaveCompetition(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Attempting to save Competition: {}", jp.getArgs()[0]);
    Object result = jp.proceed();
    logger.info("Save resulted in: {}", result);
    return result;
  }

  @Before("execution(* self.me.matchday.api.service.CompetitionService.deleteCompetitionById(..))")
  public void logDeleteCompetitionById(@NotNull JoinPoint jp) {
    logger.info("Deleting Competition with ID: [{}] from database", jp.getArgs()[0]);
  }
}
