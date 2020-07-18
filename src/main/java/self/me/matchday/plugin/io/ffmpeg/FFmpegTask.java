package self.me.matchday.plugin.io.ffmpeg;

import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

public class FFmpegTask implements Runnable {

  @Getter
  private final String command;
  @Getter
  private int exitCode;

  public FFmpegTask(@NotNull final String command) {
    this.command = command;
  }

  @SneakyThrows
  @Override
  public void run() {

    final Process process = Runtime.getRuntime().exec(command);
    // Allow the process to finish executing
    process.waitFor();
    exitCode = process.exitValue();
    process.destroy();
  }
}
