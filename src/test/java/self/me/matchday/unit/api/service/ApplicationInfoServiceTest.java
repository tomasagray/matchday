package self.me.matchday.unit.api.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.api.service.admin.ApplicationInfoService;

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
  void getApplicationInfo() {

    // given
    int minimumPid = 1_000;
    Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    Pattern systemPattern = Pattern.compile("[\\w.-]{3,}");
    Pattern ipPattern = Pattern.compile("(?:\\d{1,3}.){4}");

    // when
    logger.info("Getting application info...");
    ApplicationInfoService.ApplicationInfo applicationInfo = infoService.getApplicationInfo();
    Long pid = applicationInfo.getPid();
    String version = applicationInfo.getVersion();
    String system = applicationInfo.getSystem();
    String ip = applicationInfo.getIp();

    // then
    logger.info("Found: PID={}, Version={}, System={}, IP Address={}", pid, version, system, ip);
    assertThat(pid).isGreaterThan(minimumPid);
    final boolean versionFound = versionPattern.matcher(version).find();
    final boolean systemFound = systemPattern.matcher(system).find();
    final boolean ipFound = ipPattern.matcher(ip).find();
    assertThat(versionFound).isTrue();
    assertThat(systemFound).isTrue();
    assertThat(ipFound).isTrue();
  }
}
