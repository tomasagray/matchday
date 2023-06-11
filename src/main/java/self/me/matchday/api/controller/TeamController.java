/*
 * Copyright (c) 2022.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.api.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static self.me.matchday.api.controller.CompetitionController.IMAGE_SVG_VALUE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import self.me.matchday.api.resource.ArtworkCollectionResource;
import self.me.matchday.api.resource.ArtworkCollectionResource.ArtworkCollectionResourceAssembler;
import self.me.matchday.api.resource.ArtworkResource;
import self.me.matchday.api.resource.ArtworkResource.ArtworkResourceAssembler;
import self.me.matchday.api.resource.CompetitionResource;
import self.me.matchday.api.resource.CompetitionResource.CompetitionResourceAssembler;
import self.me.matchday.api.resource.MatchResource;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.api.resource.TeamResource;
import self.me.matchday.api.resource.TeamResource.TeamResourceAssembler;
import self.me.matchday.api.service.CompetitionService;
import self.me.matchday.api.service.MatchService;
import self.me.matchday.api.service.TeamService;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.ArtworkCollection;
import self.me.matchday.model.ArtworkRole;
import self.me.matchday.model.Image;
import self.me.matchday.model.Match;
import self.me.matchday.model.Team;

@RestController
@RequestMapping(value = "/teams")
public class TeamController {

  private final TeamService teamService;
  private final TeamResourceAssembler teamResourceAssembler;
  private final CompetitionService competitionService;
  private final CompetitionResourceAssembler competitionResourceAssembler;
  private final MatchService matchService;
  private final MatchResourceAssembler matchAssembler;
  private final ArtworkResourceAssembler artworkModeller;
  private final ArtworkCollectionResourceAssembler collectionModeller;

  public TeamController(
      TeamService teamService,
      CompetitionService competitionService,
      MatchService matchService,
      TeamResourceAssembler teamResourceAssembler,
      CompetitionResourceAssembler competitionResourceAssembler,
      MatchResourceAssembler matchAssembler,
      ArtworkResourceAssembler artworkModeller,
      ArtworkCollectionResourceAssembler collectionModeller) {

    this.teamService = teamService;
    this.teamResourceAssembler = teamResourceAssembler;
    this.competitionService = competitionService;
    this.matchService = matchService;
    this.competitionResourceAssembler = competitionResourceAssembler;
    this.matchAssembler = matchAssembler;
    this.artworkModeller = artworkModeller;
    this.collectionModeller = collectionModeller;
  }

  /**
   * Publish all Teams to the API.
   *
   * @return A List of Teams as an HttpEntity.
   */
  @RequestMapping(
      value = {"", "/"},
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<TeamResource>> fetchAllTeams(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {

    final Page<Team> teamPage = teamService.fetchAllPaged(page, size);
    final CollectionModel<TeamResource> model =
        teamResourceAssembler.toCollectionModel(teamPage.getContent());
    if (teamPage.hasNext()) {
      model.add(
          linkTo(methodOn(TeamController.class).fetchAllTeams(teamPage.getNumber() + 1, size))
              .withRel("next"));
    }
    return ResponseEntity.ok(model);
  }

  /**
   * Publish a single Team to the API, specified by the Team name.
   *
   * @param teamId The Team name (MD5 String)
   * @return The Team as an HttpEntity.
   */
  @RequestMapping(
      value = "/team/{teamId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TeamResource> fetchTeamByName(@PathVariable final UUID teamId) {

    return teamService
        .fetchById(teamId)
        .map(teamResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Retrieves all Events associated with the specified Team, and publishes to the API.
   *
   * @param teamId The name of the Team.
   * @return A CollectionModel of Events.
   */
  @RequestMapping(
      value = "/team/{teamId}/matches",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<MatchResource>> fetchEventsForTeam(
      @PathVariable final UUID teamId) {

    final List<Match> events = matchService.fetchMatchesForTeam(teamId);
    final CollectionModel<MatchResource> eventResources =
        matchAssembler
            .toCollectionModel(events)
            .add(linkTo(methodOn(TeamController.class).fetchEventsForTeam(teamId)).withSelfRel());
    return ResponseEntity.ok(eventResources);
  }

  @RequestMapping(
      value = "/team/{teamId}/competitions",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<CompetitionResource> fetchCompetitionsForTeam(
      @PathVariable final UUID teamId) {
    return competitionResourceAssembler.toCollectionModel(
        competitionService.fetchCompetitionsForTeam(teamId));
  }

  @RequestMapping(
      value = "/team/{teamId}/{role}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<ArtworkResource>> fetchTeamArtworkCollection(
      @PathVariable UUID teamId, @PathVariable ArtworkRole role) {

    final ArtworkCollection collection = teamService.fetchArtworkCollection(teamId, role);
    final CollectionModel<ArtworkResource> resources = artworkModeller.fromCollection(collection);
    resources.add(
        linkTo(methodOn(TeamController.class).fetchTeamArtworkCollection(teamId, role))
            .withSelfRel());
    resources.forEach(artwork -> TeamResourceAssembler.addArtworkLinks(teamId, role, artwork));
    return ResponseEntity.ok(resources);
  }

  @RequestMapping(
      value = "/team/{teamId}/{role}/selected",
      method = RequestMethod.GET,
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, IMAGE_SVG_VALUE})
  public ResponseEntity<byte[]> fetchSelectedArtwork(
      @PathVariable UUID teamId, @PathVariable ArtworkRole role) throws IOException {
    final Image image = teamService.fetchSelectedArtwork(teamId, role);
    return image != null
        ? ResponseEntity.ok().contentType(image.contentType()).body(image.data())
        : ResponseEntity.notFound().build();
  }

  @RequestMapping(
      value = "/team/{teamId}/{role}/selected/metadata",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkResource> fetchSelectedArtworkMetadata(
      @PathVariable UUID teamId, @PathVariable ArtworkRole role) {
    final Artwork artwork = teamService.fetchSelectedArtworkMetadata(teamId, role);
    if (artwork != null) {
      final ArtworkResource resource = artworkModeller.toModel(artwork);
      resource.add(
          linkTo(methodOn(TeamController.class).fetchSelectedArtworkMetadata(teamId, role))
              .withSelfRel());
      return ResponseEntity.ok(resource);
    }
    return ResponseEntity.notFound().build();
  }

  @RequestMapping(
      value = "/team/{teamId}/{role}/{artworkId}",
      method = RequestMethod.GET,
      produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, IMAGE_SVG_VALUE})
  public ResponseEntity<byte[]> fetchTeamArtworkImageData(
      @PathVariable UUID teamId, @PathVariable ArtworkRole role, @PathVariable Long artworkId)
      throws IOException {
    final Image image = teamService.fetchArtworkImageData(teamId, role, artworkId);
    return image != null
        ? ResponseEntity.ok().contentType(image.contentType()).body(image.data())
        : ResponseEntity.notFound().build();
  }

  @RequestMapping(
      value = "/team/{teamId}/{role}/{artworkId}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkResource> fetchTeamArtworkMetadata(
      @PathVariable UUID teamId, @PathVariable ArtworkRole role, @PathVariable Long artworkId)
      throws IOException {
    final Artwork artwork = teamService.fetchArtworkMetadata(teamId, role, artworkId);
    final ArtworkResource resource = artworkModeller.toModel(artwork);
    resource.add(
        linkTo(methodOn(TeamController.class).fetchTeamArtworkMetadata(teamId, role, artworkId))
            .withRel("metadata"));
    resource.add(
        linkTo(methodOn(TeamController.class).fetchTeamArtworkImageData(teamId, role, artworkId))
            .withRel("image"));
    return ResponseEntity.ok(resource);
  }

  @RequestMapping(
      value = "/team/{teamId}/{role}/add",
      method = RequestMethod.POST,
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkCollectionResource> addTeamArtwork(
      @PathVariable UUID teamId, @PathVariable ArtworkRole role, @RequestBody MultipartFile image)
      throws IOException {
    final ArtworkCollection collection =
        teamService.addTeamArtwork(teamId, role, Image.fromMultipartFile(image));
    final ArtworkCollectionResource resource = collectionModeller.toModel(collection);
    resource
        .getArtwork()
        .forEach(artwork -> TeamResourceAssembler.addArtworkLinks(teamId, role, artwork));
    resource.add(
        linkTo(methodOn(TeamController.class).addTeamArtwork(teamId, role, image)).withSelfRel());
    return ResponseEntity.ok(resource);
  }

  private static void addArtworkLinks(
      @NotNull ArtworkResource artwork, @NotNull UUID teamId, @NotNull ArtworkRole role) {

    try {
      final Long artworkId = artwork.getId();
      artwork.add(
          linkTo(methodOn(TeamController.class).fetchTeamArtworkImageData(teamId, role, artworkId))
              .withRel("image"));
      artwork.add(
          linkTo(methodOn(TeamController.class).fetchTeamArtworkMetadata(teamId, role, artworkId))
              .withRel("metadata"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @RequestMapping(
      value = "/team/{teamId}/{role}/{artworkId}/remove",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArtworkCollectionResource> removeTeamArtwork(
      @PathVariable UUID teamId, @PathVariable ArtworkRole role, @PathVariable Long artworkId)
      throws IOException {
    final ArtworkCollection collection = teamService.removeTeamArtwork(teamId, role, artworkId);
    final ArtworkCollectionResource resource = collectionModeller.toModel(collection);
    // add links
    resource.getArtwork().forEach(artwork -> addArtworkLinks(artwork, teamId, role));
    return ResponseEntity.ok(resource);
  }

  @RequestMapping(
      value = "/team/{teamId}/update",
      method = RequestMethod.PATCH,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TeamResource> updateTeam(@RequestBody Team team) {
    final Team update = teamService.update(team);
    final TeamResource resource = teamResourceAssembler.toModel(update);
    return ResponseEntity.ok(resource);
  }

  @RequestMapping(
      value = "/team/{teamId}/delete",
      method = RequestMethod.DELETE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<UUID> deleteTeam(@PathVariable UUID teamId) throws IOException {
    teamService.delete(teamId);
    return ResponseEntity.ok(teamId);
  }
}
