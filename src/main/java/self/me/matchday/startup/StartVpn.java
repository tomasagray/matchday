package self.me.matchday.startup;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.admin.VpnService;

@Component
public class StartVpn implements CommandLineRunner {

  private final VpnService vpnService;

  public StartVpn(VpnService vpnService) {
    this.vpnService = vpnService;
  }

  @Override
  public void run(String... args) throws Exception {
    // start VPN on application start
    vpnService.start();
  }
}
