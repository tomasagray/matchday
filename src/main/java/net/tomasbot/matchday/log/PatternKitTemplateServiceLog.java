package net.tomasbot.matchday.log;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import net.tomasbot.matchday.api.service.PatternKitTemplateService;

@Aspect
public class PatternKitTemplateServiceLog {

  private static final Logger logger = LogManager.getLogger(PatternKitTemplateService.class);

  @Around("execution(* net.tomasbot.matchday.api.service.PatternKitTemplateService.fetchAll(..))")
  public Object logFetchAllTemplates(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Retrieving all PatternKit templates...");
    final List<?> templates = (List<?>) jp.proceed();
    logger.info("Found: {} PatternKit templates", templates.size());
    return templates;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.PatternKitTemplateService.fetchById(..))")
  public void logFetchTemplateById(@NotNull JoinPoint jp) {
    logger.info("Retrieving PatternKitTemplate for ID: {}", jp.getArgs()[0]);
  }

  @Around(
      "execution(* net.tomasbot.matchday.api.service.PatternKitTemplateService.fetchByClassName(..))")
  public Object logFetchTemplateForClass(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object clazz = jp.getArgs()[0];
    logger.info("Retrieving PatternKitTemplate for Class: {}", clazz);
    Object result = jp.proceed();
    logger.debug("Found PatternKitTemplate: {} for Class: {}", result, clazz);
    return result;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.PatternKitTemplateService.save(..))")
  public void logSavePatternKitTemplate(@NotNull JoinPoint jp) {
    logger.info("Saving PatternKitTemplate: {}", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.PatternKitTemplateService.update(..))")
  public void logUpdatePatternKitTemplate(@NotNull JoinPoint jp) {
    logger.info("Updating PatternKitTemplate to: {}", jp.getArgs()[0]);
  }

  @Before("execution(* net.tomasbot.matchday.api.service.PatternKitTemplateService.delete(..))")
  public void logDeletePatternKitTemplate(@NotNull JoinPoint jp) {
    logger.info("Deleting PatternKitTemplate: {}", jp.getArgs()[0]);
  }
}
