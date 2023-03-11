package self.me.matchday.api.controller;

import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import self.me.matchday.api.resource.RestorePointResource;
import self.me.matchday.api.resource.RestorePointResource.RestorePointResourceModeller;
import self.me.matchday.api.service.admin.BackupService;
import self.me.matchday.api.service.admin.HydrationService.SystemImage;
import self.me.matchday.model.RestorePoint;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/system")
public class BackupController {

    private final BackupService backupService;
    private final RestorePointResourceModeller restorePointModeller;

    public BackupController(
            BackupService backupService,
            RestorePointResourceModeller restorePointModeller) {
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
    public RestorePointResource restoreSystem(@RequestBody UUID restorePointId) throws SQLException, IOException {
        RestorePoint restorePoint = backupService.restoreSystem(restorePointId);
        return restorePointModeller.toModel(restorePoint);
    }

    @GetMapping(value = "/dehydrate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public SystemImage dehydrateSystem() {
        return backupService.dehydrate();
    }

    @PostMapping(value = "/rehydrate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void rehydrateSystem(@RequestBody SystemImage systemImage) {
        backupService.rehydrateFrom(systemImage);
    }
}
