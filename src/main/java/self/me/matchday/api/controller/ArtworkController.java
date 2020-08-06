package self.me.matchday.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.service.ArtworkService;

@RestController
@RequestMapping(value = "/artwork")
public class ArtworkController {

  private final ArtworkService artworkService;

  @Autowired
  public ArtworkController(final ArtworkService artworkService) {
    this.artworkService = artworkService;
  }


  // ============
  // Competitions
  // ============

  /**
   * Publishes the Competition emblem image to the API.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array of the image.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/emblem",
      produces = MediaType.IMAGE_PNG_VALUE,
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionEmblem(@PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionEmblem(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the Competition fanart image to the API.
   *
   * @param competitionId The ID of the Competition
   * @return A byte array of the image data.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/fanart",
      produces = MediaType.IMAGE_JPEG_VALUE,
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionFanart(@PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionFanart(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the monochrome emblem for the Competition to the API.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array containing the image data.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/monochrome-emblem",
      produces = MediaType.IMAGE_PNG_VALUE,
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionMonochromeEmblem(
      @PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionMonochromeEmblem(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes landscape image for the Competition to the API.
   *
   * @param competitionId The ID of the Competition.
   * @return A byte array containing the image data.
   */
  @RequestMapping(
      value = "/competition/{competitionId}/landscape",
      produces = MediaType.IMAGE_JPEG_VALUE,
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchCompetitionLandscape(@PathVariable final String competitionId) {

    return
        artworkService
            .fetchCompetitionLandscape(competitionId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }

  // ============
  // Teams
  // ============

  /**
   * Publishes the Team emblem image to the API.
   *
   * @param teamId The ID of the Team
   * @return A byte array containing the image data; written to response body.
   */
  @RequestMapping(
      value = "/team/{teamId}/emblem",
      produces = MediaType.IMAGE_PNG_VALUE,
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchTeamEmblem(@PathVariable final String teamId) {

    return
        artworkService
            .fetchTeamEmblem(teamId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Publishes the fanart for the Team to the API, if available.
   *
   * @param teamId The ID of the Team.
   * @return A byte array of the image data.
   */
  @RequestMapping(
      value = "/team/{teamId}/fanart",
      produces = MediaType.IMAGE_JPEG_VALUE,
      method = RequestMethod.GET
  )
  public ResponseEntity<byte[]> fetchTeamFanart(@PathVariable final String teamId) {

    return
        artworkService
            .fetchTeamFanart(teamId)
            .map(image ->
                ResponseEntity
                    .ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(image))
            .orElse(ResponseEntity.notFound().build());
  }
}
