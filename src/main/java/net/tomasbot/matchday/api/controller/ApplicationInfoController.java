package net.tomasbot.matchday.api.controller;

import net.tomasbot.matchday.api.service.admin.ApplicationInfoService;
import net.tomasbot.matchday.model.ApplicationInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
