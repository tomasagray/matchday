package net.tomasbot.matchday.startup;

import static net.tomasbot.matchday.config.settings.UnprotectedAddress.UNPROTECTED_ADDR;

import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.api.service.admin.VpnService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class StartVpn implements CommandLineRunner {

  private final VpnService vpnService;
  private final SettingsService settingsService;

  public StartVpn(VpnService vpnService, SettingsService settingsService) {
    this.vpnService = vpnService;
    this.settingsService = settingsService;
  }

  @Override
  public void run(String... args) throws Exception {
    // start VPN on application start
    vpnService.start();

    String unprotectedIp = settingsService.getSetting(UNPROTECTED_ADDR, String.class);
    vpnService.heartbeat(unprotectedIp);
  }
}
