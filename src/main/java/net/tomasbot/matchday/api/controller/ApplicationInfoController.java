package net.tomasbot.matchday.api.controller;

import static net.tomasbot.matchday.api.resource.ApplicationInfoResource.*;

import java.io.IOException;
import net.tomasbot.matchday.api.resource.ApplicationInfoResource;
import net.tomasbot.matchday.api.service.admin.ApplicationInfoService;
import net.tomasbot.matchday.model.ApplicationInfo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationInfoController {

  private final ApplicationInfoService infoService;
  private final ApplicationInfoModeller modeller;

  public ApplicationInfoController(
      ApplicationInfoService infoService, ApplicationInfoModeller modeller) {
    this.infoService = infoService;
    this.modeller = modeller;
  }

  @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ApplicationInfoResource> getApplicationInfo() throws IOException {
    ApplicationInfo info = infoService.getApplicationInfo();
    ApplicationInfoResource model = modeller.toModel(info);
    return ResponseEntity.ok(model);
  }
}
