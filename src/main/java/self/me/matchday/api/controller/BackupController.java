package self.me.matchday.api.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.RestorePointResource;
import self.me.matchday.api.resource.RestorePointResource.RestorePointResourceModeller;
import self.me.matchday.api.service.admin.BackupService;
import self.me.matchday.api.service.admin.HydrationService.SystemImage;
import self.me.matchday.model.RestorePoint;
import self.me.matchday.util.JsonParser;

@RestController
@RequestMapping("/system")
public class BackupController {

  private final BackupService backupService;
  private final RestorePointResourceModeller restorePointModeller;

  public BackupController(
      BackupService backupService, RestorePointResourceModeller restorePointModeller) {
    this.backupService = backupService;
    this.restorePointModeller = restorePointModeller;
  }

  @GetMapping(value = "/restore-points/all", produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<RestorePointResource> getRestorePoints() {
    List<RestorePoint> restorePoints = backupService.fetchAllRestorePoints();
    return restorePointModeller.toCollectionModel(restorePoints);
  }

  @PostMapping(value = "/restore-points/create", produces = MediaType.APPLICATION_JSON_VALUE)
  public RestorePointResource createRestorePoint() throws IOException {
    RestorePoint restorePoint = backupService.createRestorePoint();
    return restorePointModeller.toModel(restorePoint);
  }

  @PostMapping(value = "/restore-points/restore", produces = MediaType.APPLICATION_JSON_VALUE)
  public RestorePointResource restoreSystem(@RequestBody UUID restorePointId)
      throws SQLException, IOException {
    RestorePoint restorePoint = backupService.restoreSystem(restorePointId);
    return restorePointModeller.toModel(restorePoint);
  }

  @GetMapping(value = "/restore-points/{restorePointId}/download", produces = "application/zip")
  public byte[] downloadRestorePoint(@PathVariable UUID restorePointId) throws IOException {
    return backupService.readBackupArchive(restorePointId);
  }

  @DeleteMapping(
      value = "/restore-points/{restorePointId}/delete",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<RestorePointResource> deleteRestorePoint(@PathVariable UUID restorePointId)
      throws IOException {
    return backupService
        .deleteRestorePoint(restorePointId)
        .map(restorePointModeller::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping(value = "/dehydrate", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public String dehydrateSystem() {
    return JsonParser.toJson(backupService.dehydrate(), SystemImage.class);
  }

  @PostMapping(value = "/rehydrate", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void rehydrateSystem(@RequestBody String systemImage) {
    backupService.rehydrateFrom(JsonParser.fromJson(systemImage, SystemImage.class));
  }
}
