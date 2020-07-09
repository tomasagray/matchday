package self.me.matchday.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.service.DataSourceService;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.util.Log;

@RestController
@RequestMapping(value = "/data-sources")
public class DataSourceController {

  private static final String LOG_TAG = "DataSourceController";

  private final DataSourceService dataSourceService;

  @Autowired
  DataSourceController(@NotNull final DataSourceService dataSourceService) {
    this.dataSourceService = dataSourceService;
  }

  @RequestMapping(value = "/refresh/all", method = {RequestMethod.POST, RequestMethod.GET},
      consumes = "application/json", produces = "application/json")
  ResponseEntity<SnapshotRequest> refreshAllSources(@RequestBody SnapshotRequest snapshotRequest) {

    Log.i(LOG_TAG,
        String.format("Refreshing all data sources with SnapshotRequest: %s\n\n", snapshotRequest));

    final SnapshotRequest status = dataSourceService.refreshDataSources(snapshotRequest);
    return
        ResponseEntity.ok().body(status);
  }
}
