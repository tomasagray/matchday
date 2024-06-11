package net.tomasbot.matchday.api.controller;

import java.io.IOException;
import net.tomasbot.matchday.api.service.admin.VpnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/vpn")
public class VpnController {

  private final VpnService vpnService;

  public VpnController(VpnService vpnService) {
    this.vpnService = vpnService;
  }

  @RequestMapping(value = "/start", method = RequestMethod.POST)
  public ResponseEntity<?> startVpnService() throws Exception {
    vpnService.start();
    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/stop", method = RequestMethod.POST)
  public ResponseEntity<?> stopVpnService() throws IOException {
    vpnService.stop();
    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/restart", method = RequestMethod.POST)
  public ResponseEntity<?> restartVpnService() throws Exception {
    vpnService.restart();
    return ResponseEntity.ok().build();
  }
}
