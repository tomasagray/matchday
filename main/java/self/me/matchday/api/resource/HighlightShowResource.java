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
import self.me.matchday.api.controller.HighlightShowController;
import self.me.matchday.api.service.MasterPlaylistService;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.PlaylistResource.PlaylistResourceAssembler;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.HighlightShow;
import self.me.matchday.model.Season;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonRootName(value = "highlight-show")
@Relation(collectionRelation = "highlight-shows")
@JsonInclude(value = Include.NON_NULL)
public class HighlightShowResource extends RepresentationModel<HighlightShowResource> {

  private Long id;
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
    private final MasterPlaylistService masterPlaylistService;
    private final PlaylistResourceAssembler playlistResourceAssembler;

    @Autowired
    public HighlightResourceAssembler(CompetitionResourceAssembler competitionResourceAssembler,
        MasterPlaylistService masterPlaylistService,
        PlaylistResourceAssembler playlistResourceAssembler) {
      super(HighlightShowController.class, HighlightShowResource.class);

      this.competitionResourceAssembler = competitionResourceAssembler;
      this.masterPlaylistService = masterPlaylistService;
      this.playlistResourceAssembler = playlistResourceAssembler;
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
      // attach playlist
      masterPlaylistService
          .fetchPlaylistById(entity.getEventId())
          .ifPresent(
              masterM3U -> highlightShowResource
                  .setPlaylists(playlistResourceAssembler.toModel(masterM3U))
          );

      // add self link
      highlightShowResource.add(
          linkTo(methodOn(HighlightShowController.class)
              .fetchHighlightById(entity.getEventId()))
              .withSelfRel());

      return highlightShowResource;
    }

    @NotNull
    @Override
    public CollectionModel<HighlightShowResource> toCollectionModel(
        @NotNull Iterable<? extends HighlightShow> entities) {

      final CollectionModel<HighlightShowResource> highlightShowResources = super
          .toCollectionModel(entities);
      highlightShowResources
          .add(linkTo(methodOn(HighlightShowController.class).fetchAllHighlights()).withSelfRel());
      return highlightShowResources;
    }
  }
}
