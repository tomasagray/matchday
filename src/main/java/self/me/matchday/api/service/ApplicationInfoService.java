package self.me.matchday.api.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApplicationInfoService {

    @Value("${application.info.version}")
    private String appVersion;

    public ApplicationInfo getApplicationInfo() {
        final Long pid = Long.parseLong(System.getProperty("PID"));
        return ApplicationInfo.builder()
                .version(appVersion)
                .system(getOsData())
                .pid(pid)
                .build();
    }

    private String getOsData() {
        final String name = System.getProperty("os.name");
        final String version = System.getProperty("os.version");
        final String arch = System.getProperty("os.arch");
        return String.format("%s %s %s", name, version, arch);
    }

    @Data
    @Builder
    public static class ApplicationInfo {
        private String version;
        private String system;
        private Long pid;
    }
}
