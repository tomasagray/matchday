package self.me.matchday.log.plugin.ffmpeg;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask;
import self.me.matchday.plugin.io.ffmpeg.FFmpegStreamTask;

// @Aspect
// todo - re-enable this class, figure out why it's throwing AjcClosure ClassNotFound exception
public class FFmpegSingleStreamTaskLog {

  private static final Logger logger = LogManager.getLogger(FFmpegSingleStreamTask.class);

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.run(..))")
  public Object logRunFfmpegStreamTask(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FFmpegStreamTask task = (FFmpegStreamTask) jp.getTarget();
    Path playlistPath = task.getPlaylistPath();
    logger.info("Running FFMPEG stream task to: {} ...", playlistPath);
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();
    long duration = Duration.between(start, end).getSeconds();
    logger.debug(
        "FFMPEG stream execution to: {} took: {}",
        playlistPath,
        String.format("%d:%02d:%02d", duration / 3600, (duration % 3600) / 60, (duration % 60)));
    return result;
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.kill(..))")
  public Object logKillFfmpegTask(@NotNull ProceedingJoinPoint jp) throws Throwable {
    FFmpegStreamTask task = (FFmpegStreamTask) jp.getTarget();
    logger.info("Killing FFMPEG stream task to: {} ...", task.getPlaylistPath());
    Boolean killed = (Boolean) jp.proceed();
    logger.info("FFMPEG stream task successfully killed? {}", killed);
    return killed;
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.getArguments(..))")
  public Object logGetFFmpegStreamTaskCommand(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object command = jp.proceed();
    logger.info("Executing FFMPEG command:\n{}", command);
    return command;
  }

  @Before("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.prepareStream(..))")
  public void logPrepareFfmpegStream() {
    logger.debug("Preparing FFMPEG stream for execution...");
  }

  @Around("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.getInputArg(..))")
  public Object logGetFfmpegInputString(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Object input = jp.proceed();
    logger.debug("Sending input to FFMPEG: {}", input);
    return input;
  }
}
