package self.me.matchday.config.settings;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Setting;

@Component
public class ArtworkStorageLocation implements Setting<Path> {

  public static final Path ARTWORK_LOCATION = Path.of("/filesystem/artwork/storage_location");

  @Value("${artwork.storage-location}")
  private Path data;

  @Override
  public Path getPath() {
    return ARTWORK_LOCATION;
  }

  @Override
  public Path getData() {
    return this.data;
  }
}
