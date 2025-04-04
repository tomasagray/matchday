package net.tomasbot.matchday.startup;

import net.tomasbot.matchday.api.service.admin.VpnService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class StartVpn implements CommandLineRunner {

  private final VpnService vpnService;

  public StartVpn(VpnService vpnService) {
    this.vpnService = vpnService;
  }

  @Override
  public void run(String... args) throws Exception {
    // start VPN on application start
    vpnService.start();
    vpnService.doHeartbeat();
  }
}
