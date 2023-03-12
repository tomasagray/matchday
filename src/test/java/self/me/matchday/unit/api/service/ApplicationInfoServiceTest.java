package self.me.matchday.unit.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.api.service.admin.ApplicationInfoService;

import java.util.regex.Pattern;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
        Pattern systemPattern = Pattern.compile("[\\w.-]+ [\\w.-]+ [\\w.-]+");

        // when
        logger.info("Getting application info...");
        ApplicationInfoService.ApplicationInfo applicationInfo = infoService.getApplicationInfo();
        Long pid = applicationInfo.getPid();
        String version = applicationInfo.getVersion();
        String system = applicationInfo.getSystem();

        // then
        logger.info("Found: PID={}, Version={}, System={}", pid, version, system);
        assertThat(pid).isGreaterThan(minimumPid);
        assertThat(version).matches(versionPattern);
        assertThat(system).matches(systemPattern);
    }
}