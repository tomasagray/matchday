package net.tomasbot.matchday.startup;

import java.util.concurrent.TimeUnit;

import net.tomasbot.matchday.api.service.admin.VpnService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
@Order
public class StartVpn implements CommandLineRunner {

  private static final int STARTUP_DELAY_S = 5;

  private final VpnService vpnService;

  public StartVpn(VpnService vpnService) {
    this.vpnService = vpnService;
  }

  /**
   * Start VPN on application start
   */
  @Override
  public void run(String... args) throws Exception {
      try {
          TimeUnit.SECONDS.sleep(STARTUP_DELAY_S);
          vpnService.start();
      } catch (Throwable e) {
          throw new RuntimeException(e);
      }
  }
}
