package self.me.matchday.api.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import self.me.matchday.api.service.MasterPlaylistService;
import self.me.matchday.api.service.VariantPlaylistService;

@RestController
public class PlaylistController {

  private static final String LOG_TAG = "PlaylistController";

  private static HttpHeaders PLAYLIST_HEADERS;

  /**
   * Initialize and return special headers for Playlists
   *
   * @return HttpHeaders with appropriate headers set
   */
  private static HttpHeaders getPlaylistHeaders() {
    // initialize playlist headers
    if (PLAYLIST_HEADERS == null) {
      PLAYLIST_HEADERS = new HttpHeaders();
      PLAYLIST_HEADERS.setContentType(MediaType.parseMediaType("application/x-mpegurl"));
      PLAYLIST_HEADERS.setAcceptCharset(List.of(StandardCharsets.UTF_8));
    }

    return PLAYLIST_HEADERS;
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
  @ResponseBody
  public ResponseEntity<String> fetchMasterPlaylistById(@PathVariable final String eventId) {

    return
        masterPlaylistService
            .fetchPlaylistById(eventId)
            .map(masterM3U ->
                new ResponseEntity<>(masterM3U.toString(), getPlaylistHeaders(), HttpStatus.OK)
            ).orElse(ResponseEntity.notFound().build());

  }

  @RequestMapping(value = {"/matches/match/{eventId}/playlist/{fileSrcId}",
      "/highlight-shows/highlight/{eventId}/playlist/{fileSrcId}"}, method = RequestMethod.GET)
  public ResponseEntity<String> fetchVariantPlaylist(@PathVariable final String eventId,
      @PathVariable final Long fileSrcId) {
    return
        variantPlaylistService
            .fetchVariantPlaylist(eventId, fileSrcId)
            .map(variantM3U ->
                new ResponseEntity<>(variantM3U.toString(), getPlaylistHeaders(), HttpStatus.OK)
            ).orElse(ResponseEntity.notFound().build());
  }
}
