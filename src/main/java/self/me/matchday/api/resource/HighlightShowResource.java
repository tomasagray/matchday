package self.me.matchday.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.controller.PlaylistController;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Season;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "highlight-show")
@Relation(collectionRelation = "highlight-shows")
@JsonInclude(value = Include.NON_NULL)
public class HighlightShowResource extends RepresentationModel<HighlightShowResource> {

  private String id;
  private String title;
  private RepresentationModel<CompetitionResource> competition;
  private RepresentationModel<PlaylistResource> playlists;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;

  @Component
  public static class HighlightResourceAssembler extends
      RepresentationModelAssemblerSupport<HighlightShow, HighlightShowResource> {

    private final CompetitionResourceAssembler competitionResourceAssembler;

    @Autowired
    public HighlightResourceAssembler(CompetitionResourceAssembler competitionResourceAssembler) {
      super(EventController.class, HighlightShowResource.class);

      this.competitionResourceAssembler = competitionResourceAssembler;
    }

    @NotNull
    @Override
    public HighlightShowResource toModel(@NotNull HighlightShow entity) {

      // instantiate resource model
      final HighlightShowResource highlightShowResource = instantiateModel(entity);

      // populate DTO
      highlightShowResource.setId(entity.getEventId());
      highlightShowResource.setTitle(entity.getTitle());
      highlightShowResource
          .setCompetition(competitionResourceAssembler.toModel(entity.getCompetition()));
      highlightShowResource.setSeason(entity.getSeason());
      highlightShowResource.setFixture(entity.getFixture());
      highlightShowResource.setDate(entity.getDate());
      // attach playlist link
      highlightShowResource.add(
          linkTo(methodOn(PlaylistController.class)
              .fetchPlaylistResourceForEvent(entity.getEventId()))
              .withRel("playlist"));
      // attach self link
      highlightShowResource.add(
          linkTo(methodOn(EventController.class).fetchHighlightById(entity.getEventId()))
              .withSelfRel());

      return highlightShowResource;
    }

    @NotNull
    @Override
    public CollectionModel<HighlightShowResource> toCollectionModel(
        @NotNull Iterable<? extends HighlightShow> entities) {

      final CollectionModel<HighlightShowResource> highlightShowResources = super
          .toCollectionModel(entities);

      // add self link
      highlightShowResources
          .add(linkTo(methodOn(EventController.class)
              .fetchAllHighlights())
              .withSelfRel());
      return highlightShowResources;
    }
  }
}
