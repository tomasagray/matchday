package self.me.matchday.api.controller;

import static self.me.matchday.config.StatusWebSocketConfigurer.BROKER_ROOT;

import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import self.me.matchday.model.VpnStatus;

@Controller
public class VpnStatusController {

  public static final String RECEIVE_ENDPOINT = "/vpn-status";
  public static final String VPN_STATUS_EMIT_ENDPOINT = BROKER_ROOT + "/vpn-status";
  // state
  @Getter @Setter private VpnStatus vpnStatus;

  @MessageMapping(RECEIVE_ENDPOINT)
  @SendTo(VPN_STATUS_EMIT_ENDPOINT)
  public VpnStatus publishVpnStatus() {
    return this.getVpnStatus();
  }
}
