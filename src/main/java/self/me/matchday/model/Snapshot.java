package self.me.matchday.model;

import java.time.Instant;
import java.util.function.Function;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Snapshot<T> {

  private final Instant timestamp;
  private final T data;

  public Snapshot(T data) {
    this.data = data;
    this.timestamp = Instant.now();
  }
  // Copy constructor
  public Snapshot(T data, Instant timestamp) {
    this.data = data;
    this.timestamp = timestamp;
  }

  public <U> Snapshot<U> map(@NotNull final Function<T, U> mapper) {
    return new Snapshot<>(mapper.apply(data));
  }
}
