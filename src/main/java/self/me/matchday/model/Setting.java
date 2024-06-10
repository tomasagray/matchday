package self.me.matchday.model;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Data;

public interface Setting<T> {

  Path getPath();

  T getData();

  @Data
  @AllArgsConstructor
  final class GenericSetting<T> implements Setting<T> {
    private Path path;
    private T data;
  }
}
