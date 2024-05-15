package self.me.matchday.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

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
