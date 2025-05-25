package net.tomasbot.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;

@Aspect
public class VpnServiceLog {

  private static final Logger logger = LogManager.getLogger(VpnServiceLog.class);

  @Before("execution(* net.tomasbot.matchday.api.service.admin.VpnService.publishVpnStatus(..))")
  public void logPublishVpnStatus(@NotNull JoinPoint jp) {
    logger.info("Publishing VPN connection status: {}", jp.getArgs());
  }

  @Around("execution(* net.tomasbot.matchday.api.service.admin.VpnService.start(..))")
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

  @Before("execution(* net.tomasbot.matchday.api.service.admin.VpnService.stop(..))")
  public void logStopVpnService() {
    logger.info("Stopping VPN service...");
  }

  @Before("execution(* net.tomasbot.matchday.api.service.admin.VpnService.restart(..))")
  public void logRestartVpnService() {
    logger.info("Restarting VPN service...");
  }

  @Around("execution(* net.tomasbot.matchday.api.service.admin.VpnService.signal(..))")
  public Object logSendSignal(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object signal = jp.getArgs()[0];
    logger.info("Sending signal to VPN: {}", signal);
    Object result = jp.proceed();
    logger.info("VPN signal `{}` got response: {}", signal, result);
    return result;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.admin.VpnService.heartbeat(..))")
  public void logScheduledVpnHeartbeatCheck() {
    logger.info("Performing VPN connection status check ...");
  }

  @Around("execution(* net.tomasbot.matchday.api.service.admin.VpnService.readConfigurations(..))")
  public Object logReadVpnConfigurations(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Reading VPN configurations...");

    Object result = jp.proceed();

    if (result instanceof Iterable<?> configurations)
      logger.info("Found {} VPN configurations.", configurations.spliterator().estimateSize());

    return result;
  }

  @Around(
          "execution(* net.tomasbot.matchday.api.service.admin.VpnService.getRandomConfiguration(..))")
  public Object logGetRandomConfig(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.debug("Getting random VPN configuration...");
    Object result = jp.proceed();
    logger.info("Using VPN configuration at: {}", result);
    return result;
  }

  @Before("execution(* net.tomasbot.matchday.api.service.admin.VpnService.handleConnectionError(..))")
  public void logConnectionError(@NotNull JoinPoint jp) throws Throwable {
    Object[] args = jp.getArgs();
    if (args != null && args.length >= 1) {
      Object arg = args[0];
      if (arg instanceof Throwable error) {
        String message = error.getMessage();
        logger.error("Error connecting to VPN: {}", message);
        logger.debug(message, error);
      }
    }
  }

  @Before("execution(* net.tomasbot.matchday.api.service.admin.VpnService.handleAmbiguousProtection(..))")
  public void logAmbiguousProtection() {
    logger.error("Could not determine unprotected IP address! VPN may not function!");
  }
}
