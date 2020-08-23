package self.me.matchday.api.controller;

import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.api.resource.FileServerResource;
import self.me.matchday.api.resource.FileServerResource.FileServerResourceAssembler;
import self.me.matchday.api.resource.FileServerUserResource;
import self.me.matchday.api.resource.FileServerUserResource.UserResourceAssembler;
import self.me.matchday.api.resource.MessageResource;
import self.me.matchday.api.resource.MessageResource.MessageResourceAssembler;
import self.me.matchday.api.service.FileServerService;
import self.me.matchday.plugin.fileserver.FileServerPlugin;
import self.me.matchday.plugin.fileserver.FileServerUser;

@RestController
@RequestMapping(value = "/file-servers")
public class FileServerController {

//  private static final String LOG_TAG = "FileServerController";

  private final FileServerService fileServerService;
  private final FileServerResourceAssembler serverResourceAssembler;
  private final UserResourceAssembler userResourceAssembler;
  private final MessageResourceAssembler messageResourceAssembler;

  @Autowired
  public FileServerController(@NotNull final FileServerService fileServerService,
      @NotNull final FileServerResourceAssembler serverResourceAssembler,
      @NotNull final UserResourceAssembler userResourceAssembler,
      @NotNull final MessageResourceAssembler messageResourceAssembler) {

    this.fileServerService = fileServerService;
    this.serverResourceAssembler = serverResourceAssembler;
    this.userResourceAssembler = userResourceAssembler;
    this.messageResourceAssembler = messageResourceAssembler;
  }

  @RequestMapping(value = "/all", method = RequestMethod.GET)
  public CollectionModel<FileServerResource> getAllFileServers() {

    final Collection<FileServerPlugin> fileServerPlugins =
        fileServerService
            .getFileServerPlugins();
    return
        serverResourceAssembler
            .toCollectionModel(fileServerPlugins);
  }

  @RequestMapping(value = "/file-server/{id}", method = RequestMethod.GET)
  public ResponseEntity<FileServerResource> getFileServerById(
      @PathVariable("id") final UUID pluginId) {

    return
        fileServerService
            .getPluginById(pluginId)
            .map(serverResourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(value = "/file-server/{id}/users", method = RequestMethod.GET)
  public ResponseEntity<CollectionModel<FileServerUserResource>> getFileServerUsers(
      @PathVariable("id") final UUID pluginId) {

    return
        fileServerService
            .getAllServerUsers(pluginId)
            .map(userResourceAssembler::toCollectionModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());

  }

  @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
  public ResponseEntity<FileServerUserResource> getUserData(
      @PathVariable("userId") final String userId) {
    return
        fileServerService
            .getUserById(userId)
            .map(userResourceAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/file-server/{id}/login",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MessageResource> loginToFileServer(
      @RequestBody final FileServerUser user,
      @PathVariable("id") final UUID fileServerId) {

    // Login to correct file server & parse response
    final ClientResponse response = fileServerService.login(user, fileServerId);
    final String messageText = getResponseMessage(response);
    final MessageResource messageResource = messageResourceAssembler.toModel(messageText);

    // Send response to end user
    return
        ResponseEntity
            .status(response.statusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageResource);
  }

  @RequestMapping(
      value = "/file-server/{id}/logout",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<MessageResource> logoutOfFileServer(
      @RequestBody final FileServerUser user,
      @PathVariable("id") final UUID fileServerId) {

    // Perform logout request
    final ClientResponse response = fileServerService.logout(user, fileServerId);
    // Extract response message
    final String responseText = getResponseMessage(response);
    final MessageResource messageResource = messageResourceAssembler.toModel(responseText);

    return
        ResponseEntity
            .status(response.statusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageResource);
  }

  @RequestMapping(
      value = "/file-server/{id}/relogin",
      method = {RequestMethod.POST, RequestMethod.GET},
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE
  )
  public @ResponseBody
  ResponseEntity<MessageResource> reloginToFileServer(
      @RequestBody final FileServerUser fileServerUser,
      @PathVariable("id") final UUID fileServerId) {

    // Perform login request
    final ClientResponse response = fileServerService.relogin(fileServerUser, fileServerId);
    // Extract message
    final String responseText = getResponseMessage(response);
    final MessageResource messageResource = messageResourceAssembler.toModel(responseText);

    return
        ResponseEntity
            .status(response.statusCode())
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageResource);
  }

  /**
   * Extract body message from a client response
   *
   * @param response The ClientResponse from the file server
   * @return The response body as a String (not null)
   */
  private @NotNull String getResponseMessage(ClientResponse response) {
    // Extract response message
    final String responseText = response.bodyToMono(String.class).block();
    // Ensure response message is not null & return
    return (responseText != null) ? responseText : "";
  }
}
