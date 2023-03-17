package self.me.matchday.model.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EntityValidator<T> {

  void validate(@Nullable T t);

  default void validateAll(@NotNull Iterable<? extends T> items) {
    items.forEach(this::validate);
  }
}
