package net.tomasbot.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.matchday.api.controller.BackupController;
import net.tomasbot.matchday.model.RestorePoint;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "restore_point")
@Relation(collectionRelation = "restore_points")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class RestorePointResource extends RepresentationModel<RestorePointResource> {

  private UUID id;
  private Timestamp timestamp;
  private Long filesize;
  private Integer eventCount;
  private Integer competitionCount;
  private Integer teamCount;
  private Integer dataSourceCount;
  private Integer userCount;

  @Component
  public static class RestorePointResourceModeller
      extends RepresentationModelAssemblerSupport<RestorePoint, RestorePointResource> {

    public RestorePointResourceModeller() {
      super(BackupController.class, RestorePointResource.class);
    }

    @Override
    public @NotNull RestorePointResource toModel(@NotNull RestorePoint entity) {
      final RestorePointResource model = instantiateModel(entity);
      model.setId(entity.getId());
      model.setTimestamp(entity.getTimestamp());
      model.setFilesize(entity.getFilesize());
      model.setEventCount(entity.getEventCount());
      model.setCompetitionCount(entity.getCompetitionCount());
      model.setTeamCount(entity.getTeamCount());
      model.setDataSourceCount(entity.getDataSourceCount());
      model.setUserCount(entity.getFileServerUserCount());
      return model;
    }
  }
}
