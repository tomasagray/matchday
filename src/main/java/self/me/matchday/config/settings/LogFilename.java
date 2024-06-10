package self.me.matchday.config.settings;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

@Component
public class LogFilename implements Setting<Path> {

  public static final Path LOG_FILENAME = Path.of("/filesystem/log_location");

  @Value("${logging.filename}")
  private Path logFilename;

  @Override
  public Path getPath() {
    return LOG_FILENAME;
  }

  @Override
  public Path getData() {
    return this.logFilename;
  }
}
