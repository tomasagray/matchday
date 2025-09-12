package net.tomasbot.matchday.api.service.admin;

import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class IpService {

  private static final Logger logger = LogManager.getLogger(IpService.class);

  private final List<ExternalIpService> ipServices = List.of(new IpInfoDotIo(), new CanYouSeeMe());

  private static @NotNull String getAddressFrom(@NotNull ExternalIpService ipService) throws IOException {
    logger.info("Attempting to determine IP address using: {}", ipService);

    String ipAddress = ipService.getIpAddress();
    if (ipAddress != null) return ipAddress;
    else throw new IOException("IP address was null");
  }

  public String getIpAddress() throws IOException {
    final int svcCount = ipServices.size();

    for (int i = 0; i < svcCount; i++) {
      ExternalIpService ipService = ipServices.get(i);

      try {
        return getAddressFrom(ipService);
      } catch (IOException e) {
        logger.error(
            "Could not determine IP address from service [{}]: {}{}",
            ipService.getName(),
            e.getMessage(),
            i < svcCount - 1 ? "; trying next..." : "");
        logger.trace(e);
      }
    }

    throw new IOException("Could not determine IP address");
  }
}
