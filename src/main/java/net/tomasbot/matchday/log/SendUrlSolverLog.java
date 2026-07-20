package net.tomasbot.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;

@Aspect
public class SendUrlSolverLog {

  private static final Logger logger = LogManager.getLogger(SendUrlSolverLog.class);

  @Around("execution(* net.tomasbot.matchday.plugin.io.url.SendUrlDotMeSolver.resolveUrl(..))")
  public Object logSolveSendUrlDotMe(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object[] args = jp.getArgs();
    logger.debug("[SendUrl.me] solving with URL: {}", args[0]);
    Object result = jp.proceed();
    logger.info("[SendUrl.me] found forward URL: {}", result);
    return result;
  }
}
