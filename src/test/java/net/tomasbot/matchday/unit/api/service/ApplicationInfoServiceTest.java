package net.tomasbot.matchday.unit.api.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;
import net.tomasbot.matchday.api.service.admin.ApplicationInfoService;
import net.tomasbot.matchday.model.ApplicationInfo;
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
@DisplayName("ApplicationInfoService validation")
class ApplicationInfoServiceTest {

  private static final Logger logger = LogManager.getLogger(ApplicationInfoServiceTest.class);

  private final ApplicationInfoService infoService;

  @Autowired
  ApplicationInfoServiceTest(ApplicationInfoService infoService) {
    this.infoService = infoService;
  }

  @Test
  @DisplayName("Validate application info")
  void getApplicationInfo() throws IOException {
    // given
    final int minimumPid = 1_000;
    final Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    final Pattern systemPattern = Pattern.compile("[\\w.-]{3,}");
    final Pattern ipPattern = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    // when
    logger.info("Getting application info...");
    ApplicationInfo applicationInfo = infoService.getApplicationInfo();

    Long pid = applicationInfo.getPid();
    String version = applicationInfo.getAppVersion();
    String system = applicationInfo.getSystem();
    String ipAddress = applicationInfo.getIpAddress();

    // then
    logger.info("Found: PID={}, Version={}, System={}, IP={}", pid, version, system, ipAddress);

    assertThat(pid).isGreaterThan(minimumPid);
    boolean versionFound = versionPattern.matcher(version).find();
    assertThat(versionFound).isTrue();
    boolean systemFound = systemPattern.matcher(system).find();
    assertThat(systemFound).isTrue();
    boolean ipFound = ipPattern.matcher(ipAddress).find();
    assertThat(ipFound).isTrue();
  }
}
