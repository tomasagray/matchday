package self.me.matchday.api.resource;


import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.controller.PlaylistController;
import self.me.matchday.db.EventFileSrcRepository;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.MasterM3U;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "playlist")
@Relation(collectionRelation = "playlists")
public class PlaylistResource extends RepresentationModel<PlaylistResource> {

  private static final LinkRelation MASTER_PLAYLIST = LinkRelation.of("master");

  @Component
  public static class PlaylistResourceAssembler extends
      RepresentationModelAssemblerSupport<MasterM3U, PlaylistResource> {

    @Autowired
    private EventFileSrcRepository eventFileSrcRepository;

    PlaylistResourceAssembler() {
      super(EventController.class, PlaylistResource.class);
    }

    @NotNull
    @Override
    public PlaylistResource toModel(@NotNull MasterM3U masterM3U) {

      final PlaylistResource playlistResource = instantiateModel(masterM3U);
      // add a self link
      playlistResource.add(
          linkTo(methodOn(PlaylistController.class)
              .fetchMasterPlaylistById(masterM3U.getId()))
              .withRel(MASTER_PLAYLIST));

      // add playlist variant links
      final Optional<List<EventFileSource>> fileSourceOptional = eventFileSrcRepository
          .findFileSourcesForEventId(masterM3U.getId());

      if (fileSourceOptional.isPresent()) {
        final List<EventFileSource> eventFileSources = fileSourceOptional.get();
        eventFileSources.forEach(eventFileSource ->
            playlistResource.add(
                linkTo(methodOn(PlaylistController.class)
                    .fetchVariantPlaylist(masterM3U.getId(), eventFileSource.getEventFileSrcId()))
                    .withRel(getFileSourceLinkRel(eventFileSource))
            )
        );
      }
      return playlistResource;
    }
  }

  /**
   * Create a LinkRelation to refer to a given File Source
   *  Format: <resolution>_<primary_language>
   * @param eventFileSource The File Source for which we need a link relation
   * @return The link relation for this file source
   */
  @NotNull
  private static LinkRelation getFileSourceLinkRel(@NotNull final EventFileSource eventFileSource) {

    final String resolution = eventFileSource.getResolution().toString().toLowerCase();
    final String language = eventFileSource.getLanguages().get(0).toLowerCase();
    return LinkRelation.of(String.format("%s_%s", resolution, language));
  }
}
