package self.me.matchday.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

abstract class EventController {

  // Link relations
  protected static final String PLAYLIST_REL = "playlist";
  protected static final String HOME_TEAM_REL = "home_team";
  protected static final String AWAY_TEAM_REL = "away_team";
  protected static final String COMP_REL = "competition";

  // Playlist headers
  protected static final MediaType MEDIA_TYPE = MediaType.parseMediaType("application/x-mpegURL");
  protected static HttpHeaders PLAYLIST_HEADERS;


  /**
   * Initialize and return special headers for Playlists
   * @return HttpHeaders with appropriate headers set
   */
  static HttpHeaders getPlaylistHeaders() {
    // initialize playlist headers
    if(PLAYLIST_HEADERS == null) {
      PLAYLIST_HEADERS = new HttpHeaders();
      PLAYLIST_HEADERS.setContentType(MEDIA_TYPE);
    }

    return PLAYLIST_HEADERS;
  }
}
