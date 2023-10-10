package self.me.matchday.log.plugin.ffmpeg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.annotation.*;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask;

@Aspect
public class FFmpegSingleStreamTaskLog {

  private static final Logger logger = LogManager.getLogger(FFmpegSingleStreamTask.class);

  @AfterReturning(
      value =
          "execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.getArguments(..))",
      returning = "args")
  public void logGetFFmpegStreamTaskCommand(@NotNull Object args) {
    logger.info("Executing FFMPEG command: {}", args);
  }

  @Before("execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.prepareStream(..))")
  public void logPrepareFfmpegStream() {
    logger.debug("Preparing single FFMPEG stream for execution...");
  }

  @AfterReturning(
      value =
          "execution(* self.me.matchday.plugin.io.ffmpeg.FFmpegSingleStreamTask.getInputArg(..))",
      returning = "input")
  public void logGetFfmpegInputString(@NotNull Object input) {
    logger.debug("Sending input to FFMPEG: {}", input);
  }
}
