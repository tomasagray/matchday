package net.tomasbot.matchday.config.settings;

import java.nio.file.Path;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VpnAutoStart implements Setting<Boolean> {

  public static final Path VPN_AUTOSTART = Path.of("/vpn/autostart/enabled");

  @Value("${system.vpn.auto-start}")
  private boolean shouldAutoStart;

  @Override
  public Path getPath() {
    return VPN_AUTOSTART;
  }

  @Override
  public Boolean getData() {
    return this.shouldAutoStart;
  }
}
