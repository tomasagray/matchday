package net.tomasbot.matchday.model.video;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Data
public class VideoStreamingError {

  private final String message;
  private final List<StackTraceElement> stackTrace;
  @Getter private final Timestamp timestamp;
  private VideoStreamingError cause;

  public VideoStreamingError(@NotNull Throwable e) {
    this.message = e.getMessage();
    this.stackTrace = getStackTrace(e);
    this.timestamp = Timestamp.from(Instant.now());

    final Throwable cause = e.getCause();
    if (cause != null) {
      this.cause = new VideoStreamingError(cause);
    }
  }

  private static List<StackTraceElement> getStackTrace(@NotNull Throwable cause) {
    return Arrays.stream(cause.getStackTrace())
        .map(StackTraceElement::new)
        .collect(Collectors.toList());
  }

  @Data
  public static class StackTraceElement {
    private final String clazz;
    private final String methodName;
    private final String filename;
    private final int lineNumber;

    public StackTraceElement(java.lang.@NotNull StackTraceElement e) {
      this.clazz = e.getClassName();
      this.methodName = e.getMethodName();
      this.filename = e.getFileName();
      this.lineNumber = e.getLineNumber();
    }
  }
}
