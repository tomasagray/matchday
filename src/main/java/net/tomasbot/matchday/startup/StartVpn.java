package net.tomasbot.matchday.startup;

import static net.tomasbot.matchday.config.settings.VpnAutoStart.VPN_AUTOSTART;

import java.util.concurrent.TimeUnit;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.api.service.admin.VpnService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order
public class StartVpn implements CommandLineRunner {

  private static final int STARTUP_DELAY_S = 5;

  private final VpnService vpnService;
  private final SettingsService settingsService;

  public StartVpn(VpnService vpnService, SettingsService settingsService) {
    this.vpnService = vpnService;
    this.settingsService = settingsService;
  }

  /**
   * Start VPN on application start
   */
  @Override
  public void run(String... args) throws Exception {
    try {
      boolean shouldAutoStart = settingsService.getSetting(VPN_AUTOSTART, Boolean.class);
      if (shouldAutoStart) {
        TimeUnit.SECONDS.sleep(STARTUP_DELAY_S);
        vpnService.start();
      }
    } catch (Throwable e) {
        throw new IllegalStateException(e);
    }
  }
}
