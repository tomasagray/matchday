package self.me.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.admin.VpnService;

@Aspect
public class VpnServiceLog {

  private static final Logger logger = LogManager.getLogger(VpnService.class);

  @Around("execution(* self.me.matchday.api.service.admin.VpnService.start(..))")
  public Object logStartVpnService(@NotNull ProceedingJoinPoint jp) throws Throwable {
    try {
      logger.info("Starting VPN service...");
      Object result = jp.proceed();
      logger.info("VPN service started.");
      return result;
    } catch (Exception e) {
      String message = "Error running VPN service: " + e.getMessage();
      logger.error(message);
      logger.trace(message, e);
      return null;
    }
  }

  @AfterReturning(
      value = "execution(* self.me.matchday.api.service.admin.VpnService.getStartupCommand(..))",
      returning = "cmd")
  public void logVpnStartupCommand(@NotNull Object cmd) {
    logger.debug("Starting VPN service with command:\n\t{}", cmd);
  }

  @Before("execution(* self.me.matchday.api.service.admin.VpnService.stop(..))")
  public void logStopVpnService() {
    logger.info("Stopping VPN service...");
  }

  @Before("execution(* self.me.matchday.api.service.admin.VpnService.restart(..))")
  public void logRestartVpnService() {
    logger.info("Restarting VPN service...");
  }

  @Around("execution(* self.me.matchday.api.service.admin.VpnService.signal(..))")
  public Object logSendSignal(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object signal = jp.getArgs()[0];
    logger.info("Sending signal to VPN: {}", signal);
    Object result = jp.proceed();
    logger.info("VPN signal `{}` got response: {}", signal, result);
    return result;
  }
}
