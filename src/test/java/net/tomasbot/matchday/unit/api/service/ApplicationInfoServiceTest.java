package net.tomasbot.matchday.unit.api.service;

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
import net.tomasbot.matchday.api.service.admin.ApplicationInfoService;
import net.tomasbot.matchday.model.ApplicationInfo;

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
    final int minimumPid = 1_000;
    final Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
    final Pattern systemPattern = Pattern.compile("[\\w.-]{3,}");

    // when
    logger.info("Getting application info...");
    ApplicationInfo applicationInfo = infoService.getApplicationInfo();
    Long pid = applicationInfo.getPid();
    String version = applicationInfo.getVersion();
    String system = applicationInfo.getSystem();

    // then
    logger.info("Found: PID={}, Version={}, System={}", pid, version, system);
    assertThat(pid).isGreaterThan(minimumPid);
    final boolean versionFound = versionPattern.matcher(version).find();
    final boolean systemFound = systemPattern.matcher(system).find();
    assertThat(versionFound).isTrue();
    assertThat(systemFound).isTrue();
  }
}
