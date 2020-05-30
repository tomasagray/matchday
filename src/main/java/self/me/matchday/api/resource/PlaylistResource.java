package self.me.matchday.api.resource;


import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import self.me.matchday.api.service.EventService;
import self.me.matchday.model.Event;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;
import self.me.matchday.model.MasterM3U;
import self.me.matchday.util.Log;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "playlist")
@Relation(collectionRelation = "playlists")
public class PlaylistResource extends RepresentationModel<PlaylistResource> {

  private static final String LOG_TAG = "PlaylistResource";
  // Default markers
  private static final LinkRelation MASTER_PLAYLIST = LinkRelation.of("master");
  private static final String UNKNOWN_RESOLUTION = "res-unknown";
  private static final String UNKNOWN_LANGUAGE = "lang-unknown";

  @Component
  public static class PlaylistResourceAssembler extends
      RepresentationModelAssemblerSupport<MasterM3U, PlaylistResource> {

    private final EventService eventService;

    @Autowired
    PlaylistResourceAssembler(EventService eventService) {

      super(EventController.class, PlaylistResource.class);
      this.eventService = eventService;
    }

    @NotNull
    @Override
    public PlaylistResource toModel(@NotNull MasterM3U masterM3U) {

      final PlaylistResource playlistResource = instantiateModel(masterM3U);
      // add a self link
      playlistResource.add(
          linkTo(methodOn(PlaylistController.class)
              .fetchMasterPlaylistById(masterM3U.getEventId()))
              .withRel(MASTER_PLAYLIST));

      // Get the file sources for this Event
      final Optional<Event> eventOptional = eventService.fetchById(masterM3U.getEventId());
      if (eventOptional.isPresent()) {
        final Event event = eventOptional.get();
        final Set<EventFileSource> eventFileSources = event.getFileSources();

        // Add links to variants
        eventFileSources.forEach(eventFileSource ->
            playlistResource.add(
                linkTo(methodOn(PlaylistController.class)
                    .fetchVariantPlaylist(masterM3U.getEventId(), eventFileSource.getEventFileSrcId()))
                    .withRel(getFileSourceLinkRel(eventFileSource))
            )
        );
      } else {
        Log.d(LOG_TAG,
            String.format("No variant playlists found for Event: %s", masterM3U.getEventId()));
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

    // Possible null fields
    final Resolution resolution = eventFileSource.getResolution();
    final List<String> languages = eventFileSource.getLanguages();
    // Get the markers
    final String resMarker =
        (resolution != null)
            ? resolution.toString().toLowerCase()
            : UNKNOWN_RESOLUTION;
    final String language =
        ((languages != null) && (languages.size() != 0))
            ? languages.get(0).toLowerCase()
            : UNKNOWN_LANGUAGE;

    // Return the formatted relation string
    return LinkRelation.of(String.format("%s_%s", resMarker, language));
  }
}
