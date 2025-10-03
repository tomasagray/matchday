package net.tomasbot.matchday.admin.api.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import net.tomasbot.matchday.api.service.admin.IpService;
import net.tomasbot.matchday.api.service.admin.VpnService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class VpnServiceTest {

  private static final Logger logger = LogManager.getLogger(VpnServiceTest.class);

  private final VpnService vpnService;
  private final IpService ipService;

  @Autowired
  VpnServiceTest(VpnService vpnService, IpService ipService) {
    this.vpnService = vpnService;
    this.ipService = ipService;
  }

  @Test
  @DisplayName("Validate VPN service can be started")
  void testVpnServiceStartup() throws Throwable {
    // given
    logger.info("Starting VPN service for test...");
    final String beforeTestIp = ipService.getIpAddress();
    assertThat(beforeTestIp).isNotNull().isNotEmpty();
    logger.info("Before starting VPN, IP is: {}", beforeTestIp);

    // when
    vpnService.start();
    logger.info("VPN for test successfully started.");

    // then
    final String afterTestIp = ipService.getIpAddress();
    assertThat(afterTestIp).isNotNull().isNotEmpty();
    logger.info("After starting VPN, IP is: {}", afterTestIp);
    assertThat(beforeTestIp).isNotEqualTo(afterTestIp);
  }
}
