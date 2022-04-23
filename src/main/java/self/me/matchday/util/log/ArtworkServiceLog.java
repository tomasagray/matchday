package self.me.matchday.util.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.ArtworkService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Aspect
public class ArtworkServiceLog {

  private static final List<Integer> defaultSizes = List.of(15_182, 13_989);

  private static final Logger logger = LogManager.getLogger(ArtworkService.class);

  @SuppressWarnings("unchecked cast")
  @Around("execution(* self.me.matchday.api.service.ArtworkService.fetch*(..))")
  public Object logFetchArtwork(@NotNull ProceedingJoinPoint jp) throws Throwable {

    final Object arg = jp.getArgs()[0];
    final List<String> action = getAction(jp);
    final String actionType = action.get(0);
    final String actionName = String.join(" ", action);
    logger.info("Fetching {} for {}: {}...", actionName, actionType, arg);

    final Optional<byte[]> result = (Optional<byte[]>) jp.proceed();
    result.ifPresentOrElse(
        img -> {
          if (defaultSizes.contains(img.length)) {
            logger.info("Emblem not found for {}: {}; returning default", actionType, arg);
          } else {
            logger.info("Emblem found for {}: {}, size: {}", actionType, arg, img.length);
          }
          // todo: what to do with errors?
        },
        () -> logger.warn("{}: {} not found in database", actionType, arg));
    return result;
  }

  @NotNull
  private List<String> getAction(@NotNull ProceedingJoinPoint jp) {
    return Arrays.stream(jp.getSignature().getName().split("(?<!^)(?=[A-Z])"))
        .skip(1)
        .collect(Collectors.toList());
  }
}
