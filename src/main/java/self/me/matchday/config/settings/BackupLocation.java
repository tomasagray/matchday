package self.me.matchday.config.settings;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

@Component
public class BackupLocation implements Setting<Path> {

  public static final Path BACKUP_LOCATION = Path.of("/filesystem/backup_location");

  @Value("${application.backup-location}")
  private Path backupLocation;

  @Override
  public Path getPath() {
    return BACKUP_LOCATION;
  }

  @Override
  public Path getData() {
    return this.backupLocation;
  }
}
