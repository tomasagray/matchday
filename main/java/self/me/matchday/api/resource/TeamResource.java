package self.me.matchday.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.TeamController;
import self.me.matchday.model.Team;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "team")
@Relation(collectionRelation = "teams")
@JsonInclude(value = Include.NON_NULL)
public class TeamResource extends RepresentationModel<TeamResource> {

  private Long id;
  private String name;
  private String abbreviation;
  private Locale locale;

  @Component
  public static class TeamResourceAssembler extends
      RepresentationModelAssemblerSupport<Team, TeamResource> {

    private static final LinkRelation EMBLEM = LinkRelation.of("emblem");

    public TeamResourceAssembler() {
      super(TeamController.class, TeamResource.class);
    }

    @NotNull
    @Override
    public TeamResource toModel(@NotNull Team team) {

      final TeamResource teamResource = instantiateModel(team);
      // initialize resource
      teamResource.setId(team.getTeamId());
      teamResource.setName(team.getName());
      teamResource.setAbbreviation(team.getAbbreviation());
      teamResource.setLocale(team.getLocale());
      // attach links
      teamResource.add(
          linkTo(methodOn(TeamController.class).fetchTeamById(team.getTeamId())).withSelfRel());
      teamResource.add(linkTo(methodOn(TeamController.class).fetchTeamEmblemUrl(team.getTeamId()))
          .withRel(EMBLEM));

      return teamResource;
    }

    @NotNull
    @Override
    public CollectionModel<TeamResource> toCollectionModel(
        @NotNull Iterable<? extends Team> teams) {
      final CollectionModel<TeamResource> teamResources = super.toCollectionModel(teams);
      // add a self link
      teamResources.add(linkTo(methodOn(TeamController.class).fetchAllTeams()).withSelfRel());
      return teamResources;
    }
  }
}
