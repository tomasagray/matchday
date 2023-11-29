package self.me.matchday.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class VpnStatus {

  private final VpnConnectionStatus connectionStatus;
  private final String ipAddress;
  private String vpnServer = "";
  public enum VpnConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
  }
}
