package net.tomasbot.matchday.config.settings;

import java.net.URL;
import java.nio.file.Path;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FlaresolverrUrl implements Setting<URL> {

  public static final Path FLARESOLVERR_URL = Path.of("/system/services/flaresolverr/url");

  @Value("${system.services.flaresolverr.url}")
  private URL flareUrl;

  @Override
  public Path getPath() {
    return FLARESOLVERR_URL;
  }

  @Override
  public URL getData() {
    return this.flareUrl;
  }
}
