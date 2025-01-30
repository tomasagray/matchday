package net.tomasbot.matchday.api.service.video;

public class AdditionalVideoArgException extends IllegalArgumentException {
  public AdditionalVideoArgException(int argsCount) {
    super("Number of additional FFmpeg arguments must be even, was " + argsCount);
  }
}
