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

    private static final LinkRelation EMBLEM = LinkRelation.of("emblem");

    public CompetitionResourceAssembler() {
      super(CompetitionService.class, CompetitionResource.class);
    }

    @NotNull
    @Override
    public CompetitionResource toModel(@NotNull Competition competition) {

      final CompetitionResource competitionResource = instantiateModel(competition);

      // populate DTO
      competitionResource.setId(competition.getCompetitionId());
      competitionResource.setName(competition.getName());
      competitionResource.setAbbreviation(competition.getAbbreviation());
      competitionResource.setLocale(competition.getLocale());
      // attach links
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionById(competition.getCompetitionId())).withSelfRel());
      competitionResource.add(linkTo(methodOn(CompetitionController.class)
          .fetchCompetitionEmblemUrl(competition.getCompetitionId())).withRel(EMBLEM));

      return competitionResource;
    }

    @NotNull
    @Override
    public CollectionModel<CompetitionResource> toCollectionModel(
        @NotNull Iterable<? extends Competition> competitions) {

      return super.toCollectionModel(competitions);
    }
  }

}