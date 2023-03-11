package self.me.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.BackupController;
import self.me.matchday.model.RestorePoint;

import java.sql.Timestamp;
import java.util.UUID;

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
            model.setUserCount(entity.getUserCount());
            return model;
        }
    }
}
