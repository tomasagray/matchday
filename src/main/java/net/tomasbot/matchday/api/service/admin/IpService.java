package net.tomasbot.matchday.api.service.admin;

import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IpService {

  private final List<ExternalIpService> externalIpServices =
      List.of(new IpInfo(), new CanYouSeeMe());

  public String getIpAddress() throws IOException {
    for (ExternalIpService externalIpService : externalIpServices) {
      String ipAddress = externalIpService.getIpAddress();
      if (ipAddress != null) {
        return ipAddress;
      }
    }

    throw new IOException("Could not determine IP address");
  }
}
