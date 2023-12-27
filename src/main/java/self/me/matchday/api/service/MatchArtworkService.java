package self.me.matchday.api.service;

import java.io.IOException;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import self.me.matchday.model.*;
import self.me.matchday.util.ResourceFileReader;

@Service
public class MatchArtworkService {

  private static final Color DEFAULT_HOME_COLOR = new Color(35, 38, 45);
  private static final Color DEFAULT_AWAY_COLOR = new Color(46, 50, 57);
  private static final String DEFAULT_TEAM_EMBLEM = "image/default_team_emblem.png";

  private final ArtworkService artworkService;

  public MatchArtworkService(ArtworkService artworkService) {
    this.artworkService = artworkService;
  }

  private static byte[] readDefaultTeamEmblem() throws IOException {
    return ResourceFileReader.readBinaryData(DEFAULT_TEAM_EMBLEM);
  }

  public Artwork makeMatchArtwork(@NotNull Match match) throws IOException {
    final Artwork existingArtwork = match.getArtwork();
    if (existingArtwork != null) {
      match.setArtwork(null);
      artworkService.deleteArtwork(existingArtwork);
    }
    final Collection<Param<?>> params = createMatchArtworkParams(match);
    return artworkService.createArtwork(Match.class, params);
  }

  private @NotNull @Unmodifiable Collection<Param<?>> createMatchArtworkParams(@NotNull Match match)
      throws IOException {
    final Team homeTeam = match.getHomeTeam();
    final Team awayTeam = match.getAwayTeam();
    // emblems
    final Param<?> homeTeamEmblem = createTeamEmblemParam(homeTeam, "#home-team-emblem");
    final Param<?> awayTeamEmblem = createTeamEmblemParam(awayTeam, "#away-team-emblem");
    // colors
    final Color[] teamColors = getContrastingTeamColors(homeTeam, awayTeam);
    final Param<Color> homeTeamColor = new Param<>("#home-team-color", teamColors[0]);
    final Param<Color> awayTeamColor = new Param<>("#away-team-color", teamColors[1]);
    // other
    final Param<MediaType> type = new Param<>("#type", MediaType.IMAGE_PNG);

    return List.of(homeTeamEmblem, awayTeamEmblem, homeTeamColor, awayTeamColor, type);
  }

  private @NotNull Param<?> createTeamEmblemParam(@NotNull Team team, @NotNull String tag)
      throws IOException {
    final Artwork emblem = team.getEmblem().getSelected();
    byte[] data;
    if (emblem != null && emblem.getFile() != null) {
      final Image image = artworkService.readArtworkData(emblem);
      data = image.data();
    } else {
      data = readDefaultTeamEmblem();
    }
    return new Param<>(tag, data);
  }

  private Color @NotNull [] getContrastingTeamColors(@NotNull Team home, @NotNull Team away) {
    final List<Color> homeColors = home.getColors();
    final List<Color> awayColors = away.getColors();
    final Color[] colorPair = artworkService.getContrastingColorPair(homeColors, awayColors);
    return colorPair != null ? colorPair : new Color[] {DEFAULT_HOME_COLOR, DEFAULT_AWAY_COLOR};
  }

  public Image readArtworkData(@NotNull Artwork artwork) throws IOException {
    return artworkService.readArtworkData(artwork);
  }

  public void deleteArtworkFromDisk(Artwork artwork) throws IOException {
    artworkService.deleteArtworkFromDisk(artwork);
  }
}
