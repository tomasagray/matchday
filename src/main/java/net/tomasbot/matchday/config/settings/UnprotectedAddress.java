package net.tomasbot.matchday.config.settings;

import java.nio.file.Path;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UnprotectedAddress implements Setting<String> {

  public static final Path UNPROTECTED_ADDR = Path.of("/system/network/address/unprotected");

  @Value("${system.network.address.unprotected}")
  private String unprotected;

  @Override
  public Path getPath() {
    return UNPROTECTED_ADDR;
  }

  @Override
  public String getData() {
    return this.unprotected;
  }
}
