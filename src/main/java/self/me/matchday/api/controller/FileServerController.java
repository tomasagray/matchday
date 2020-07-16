package self.me.matchday.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.plugin.fileserver.FSUser;

@RestController
@RequestMapping(value = "/file-servers")
public class FileServerController {

  private final FileServerService fileServerService;

  @Autowired
  public FileServerController(@NotNull final FileServerService fileServerService) {
    this.fileServerService = fileServerService;
  }

  @RequestMapping(value = "/file-server/{id}/login", method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Boolean> loginToFileServer(@RequestBody final FSUser user,
      @PathVariable final Integer id) {

    // Login to correct file server
    final boolean login = fileServerService.login(user, id);

    if (login) {
      return ResponseEntity.ok().body(true);
    } else {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(false);
    }
  }
}
