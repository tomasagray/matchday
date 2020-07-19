package self.me.matchday.plugin.io.ffmpeg;

import java.io.File;
import java.util.List;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

@Data
public class FFmpegTask implements Runnable {

  private final String command;
  private final List<String> args;
  private int exitCode;
  private File outputFile;

  public FFmpegTask(@NotNull final String command, @NotNull final List<String> args) {
    this.command = command;
    this.args = args;
  }

  @SneakyThrows
  @Override
  public void run() {

    // Collate program arguments
    final String arguments = Strings.join(args, ' ');
    final String execCommand = String.format("%s %s \"%s\"", command, arguments, outputFile);
    final Process process = Runtime.getRuntime().exec(execCommand);
    // Allow the process to finish executing
    process.waitFor();
    exitCode = process.exitValue();
    process.destroy();
  }
}