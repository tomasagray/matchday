package self.me.matchday.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.service.admin.ApplicationInfoService;
import self.me.matchday.api.service.admin.ApplicationInfoService.ApplicationInfo;

@RestController
public class ApplicationInfoController {

    private final ApplicationInfoService infoService;

    public ApplicationInfoController(ApplicationInfoService infoService) {
        this.infoService = infoService;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApplicationInfo getApplicationInfo() {
        return infoService.getApplicationInfo();
    }
}
