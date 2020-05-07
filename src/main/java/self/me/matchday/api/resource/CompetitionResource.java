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
import self.me.matchday.api.controller.CompetitionController;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.model.Competition;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "competition")
@Relation(collectionRelation = "competitions")
@JsonInclude(value = Include.NON_NULL)
public class CompetitionResource extends RepresentationModel<CompetitionResource> {

  private String id;
  private String name;
  private String abbreviation;
  private Locale locale;

  @Component
  public static class CompetitionResourceAssembler extends
      RepresentationModelAssemblerSupport<Competition, CompetitionResource> {

    private static final LinkRelation TEAMS = LinkRelation.of("teams");
    private static final LinkRelation EMBLEM = LinkRelation.of("emblem");
    private static final LinkRelation FANART = LinkRelation.of("fanart");
    private static final LinkRelation MONOCHROME = LinkRelation.of("monochrome_emblem");
    private static final LinkRelation LANDSCAPE = LinkRelation.of("landscape");
    private static final LinkRelation EVENTS = LinkRelation.of("events");

    public CompetitionResourceAssembler() {
      super(CompetitionService.class, CompetitionResource.class);
    }

    @NotNull
    @Override
    public CompetitionResource toModel(@NotNull Competition competition) {

      final CompetitionResource competitionResource = instantiateModel(competition);

      // populate DTO
      final String competitionId = competition.getCompetitionId();
      competitionResource.setId(competitionId);
      competitionResource.setName(competition.getName());
      competitionResource.setAbbreviation(competition.getAbbreviation());
      competitionResource.setLocale(competition.getLocale());
      // attach links
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionById(competitionId)).withSelfRel());
      // teams
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionTeams(competitionId)).withRel(TEAMS));
      // artwork
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionEmblem(competitionId)).withRel(EMBLEM));
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionFanart(competitionId)).withRel(FANART));
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionMonochromeEmblem(competitionId)).withRel(MONOCHROME));
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionLandscape(competitionId)).withRel(LANDSCAPE));
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionEvents(competitionId)).withRel(EVENTS));

      return competitionResource;
    }

    @NotNull
    @Override
    public CollectionModel<CompetitionResource> toCollectionModel(
        @NotNull Iterable<? extends Competition> competitions) {

      final CollectionModel<CompetitionResource> competitionResources = super
          .toCollectionModel(competitions);
      // add a self link
      competitionResources
          .add(linkTo(methodOn(CompetitionController.class)
              .fetchAllCompetitions())
              .withSelfRel());
      return competitionResources;
    }
  }

}
