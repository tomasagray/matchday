package net.tomasbot.matchday.config.settings;

import java.nio.file.Path;
import net.tomasbot.matchday.model.Setting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
public class VpnHeartbeat implements Setting<CronTrigger> {

  public static final Path VPN_HEARTBEAT = Path.of("/tasks/vpn_heartbeat");

  @Value("${scheduled-tasks.cron.vpn-heartbeat-data}")
  private CronTrigger vpnHeartbeat;

  @Override
  public Path getPath() {
    return VPN_HEARTBEAT;
  }

  @Override
  public CronTrigger getData() {
    return this.vpnHeartbeat;
  }
}
