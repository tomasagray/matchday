package net.tomasbot.matchday.log.plugin.parsing;

import net.tomasbot.matchday.model.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;

@Aspect
public class DataSourceParsingLog {

  private static final Logger logger = LogManager.getLogger(DataSourceParsingLog.class);

  @Before(
      "execution(* net.tomasbot.matchday.plugin.datasource.parsing.HypertextEntityParser.getEntityStream(..))")
  public void logGetHypertextEntityStream(@NotNull JoinPoint jp) {
    Object[] args = jp.getArgs();
    if (args.length == 0) return;

    if (args[0] instanceof final DataSource<?> dataSource) {
      logger.debug(
          "Parsing data with DataSource: {} [{}]",
          dataSource.getTitle(),
          dataSource.getDataSourceId());
    }
  }

  @Around("execution(* net.tomasbot.matchday.plugin.datasource.parsing.TextParser.parseEntity(..))")
  public Object logParseEntity(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object[] args = jp.getArgs();
    logger.debug("Parsing entity with PatternKit: {}", args[0]);
    Object result = jp.proceed();
    logger.info("Parsed entity: {}", result);
    return result;
  }
}
