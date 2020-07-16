package self.me.matchday.api.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.PlaylistResource;
import self.me.matchday.api.service.MasterPlaylistService;
import self.me.matchday.api.service.VariantPlaylistService;

@RestController
public class PlaylistController {

  // Initialize  special headers for Playlists
  private static final HttpHeaders PLAYLIST_HEADERS;
  static  {
    PLAYLIST_HEADERS = new HttpHeaders();
    PLAYLIST_HEADERS.setContentType(MediaType.parseMediaType("application/x-mpegurl"));
    PLAYLIST_HEADERS.setAcceptCharset(List.of(StandardCharsets.UTF_8));
  }

  private final MasterPlaylistService masterPlaylistService;
  private final VariantPlaylistService variantPlaylistService;

  @Autowired
  public PlaylistController(MasterPlaylistService masterPlaylistService,
      VariantPlaylistService variantPlaylistService) {

    this.masterPlaylistService = masterPlaylistService;
    this.variantPlaylistService = variantPlaylistService;
  }

  @RequestMapping(value = {"/matches/match/{eventId}/playlist",
      "/highlight-shows/highlight/{eventId}/playlist"}, method = RequestMethod.GET)
  public ResponseEntity<PlaylistResource> fetchPlaylistResourceForEvent(@PathVariable String eventId) {

    return
      masterPlaylistService
          .fetchPlaylistResourceForEvent(eventId)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(value = {"/matches/match/{eventId}/playlist/master",
      "/highlight-shows/highlight/{eventId}/playlist/master"}, method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<String> fetchMasterPlaylistById(@PathVariable final String eventId) {

    return
        masterPlaylistService
            .fetchMasterPlaylistForEvent(eventId)
            .map(masterM3U ->
                new ResponseEntity<>(masterM3U.toString(), PLAYLIST_HEADERS, HttpStatus.OK)
            ).orElse(ResponseEntity.notFound().build());

  }

  @RequestMapping(value = {"/matches/match/{eventId}/playlist/{fileSrcId}",
      "/highlight-shows/highlight/{eventId}/playlist/{fileSrcId}"}, method = RequestMethod.GET)
  public ResponseEntity<String> fetchVariantPlaylist(@PathVariable final String eventId,
      @PathVariable final UUID fileSrcId) {

    return
        variantPlaylistService
            .fetchVariantPlaylist(eventId, fileSrcId)
            .map(variantM3U ->
                new ResponseEntity<>(variantM3U.toString(), PLAYLIST_HEADERS, HttpStatus.OK)
            ).orElse(ResponseEntity.notFound().build());
  }
}
