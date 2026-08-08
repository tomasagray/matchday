package net.tomasbot.matchday.common.flaresolverr;

import java.net.URL;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public final class FlaresolverrRequestData {

  private final String cmd = "request.get";
  private final URL url;
  private final Long maxTimeout;

  public FlaresolverrRequestData(@NotNull URL url, Long maxTimeout) {
    this.url = url;
    this.maxTimeout = maxTimeout;
  }
}
