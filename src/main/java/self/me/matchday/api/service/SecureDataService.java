package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class SecureDataService {

  // TODO : Implement this for reals

  public @NotNull <T> T encryptData(@NotNull final T data) {
    return data;
  }

  public @NotNull <T> T decryptData(@NotNull final T data) {
    return data;
  }

}
