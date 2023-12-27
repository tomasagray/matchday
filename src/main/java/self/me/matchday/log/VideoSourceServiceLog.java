package self.me.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.video.VideoSourceService;

@Aspect
public class VideoSourceServiceLog {

  private final Logger logger = LogManager.getLogger(VideoSourceService.class);

  @Around("execution(* self.me.matchday.api.service.video.VideoSourceService.fetchById(..))")
  public Object logFetchVideoFileSource(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Fetching VideoFileSource with ID: {} from database...", jp.getArgs()[0]);
    Object result = jp.proceed();
    logger.info("... Retrieved VideoFileSource: {}", result);
    return result;
  }
}
