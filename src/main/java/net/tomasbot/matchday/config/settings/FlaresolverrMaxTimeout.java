package net.tomasbot.matchday.config.settings;

import java.nio.file.Path;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FlaresolverrMaxTimeout implements Setting<Long> {

  public static final Path FLARESOLVERR_TIMEOUT =
      Path.of("/system/services/flaresolverr/max-timeout");

  @Value("${system.services.flaresolverr.max-timeout}")
  private Long maxTimeout;

  @Override
  public Path getPath() {
    return FLARESOLVERR_TIMEOUT;
  }

  @Override
  public Long getData() {
    return this.maxTimeout;
  }
}
