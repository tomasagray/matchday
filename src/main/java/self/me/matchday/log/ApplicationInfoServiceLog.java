package self.me.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.admin.ApplicationInfoService;

@Aspect
public class ApplicationInfoServiceLog {

  private static final Logger logger = LogManager.getLogger(ApplicationInfoService.class);

  @Around(
      "execution(* self.me.matchday.api.service.admin.ApplicationInfoService.getApplicationInfo(..))")
  public Object logGetApplicationInfo(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Getting application info...");
    final Object info = jp.proceed();
    logger.debug("Application Info is: {}", info);
    return info;
  }
}
