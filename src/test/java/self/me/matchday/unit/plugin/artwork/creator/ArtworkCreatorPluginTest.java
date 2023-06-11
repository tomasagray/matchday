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

package self.me.matchday.unit.plugin.artwork.creator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Color;
import self.me.matchday.model.Match;
import self.me.matchday.model.Param;
import self.me.matchday.plugin.artwork.creator.ArtworkCreatorPlugin;
import self.me.matchday.util.ResourceFileReader;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Testing & validation for Artwork creation plugin")
class ArtworkCreatorPluginTest {

  private static final Logger logger = LogManager.getLogger(ArtworkCreatorPluginTest.class);
  private final ArtworkCreatorPlugin plugin;

  @Autowired
  ArtworkCreatorPluginTest(ArtworkCreatorPlugin plugin) {
    this.plugin = plugin;
  }

  @Test
  @DisplayName("Validate plugin has correct ID")
  void getPluginId() {
    // given
    final UUID expectedId = UUID.fromString("d7e1e55e-5daa-4dbf-970b-9eac4901f880");
    logger.info("Expected ArtworkCreatorPlugin ID: {}", expectedId);
    // when
    final UUID actualId = plugin.getPluginId();
    logger.info("Got ArtworkCreatorPlugin ID: {}", actualId);
    // then
    assertThat(actualId).isEqualTo(expectedId);
  }

  @Test
  @DisplayName("Validate plugin title")
  void getTitle() {
    // given
    final String expectedTitle = "Artwork Creator Plugin";
    logger.info("Expecting plugin title: {}", expectedTitle);
    // when
    final String actualTitle = plugin.getTitle();
    logger.info("Found ArtworkCreatorPlugin title: {}", actualTitle);
    // then
    assertThat(actualTitle).isEqualTo(expectedTitle);
  }

  @Test
  @DisplayName("Validate plugin description")
  void getDescription() {
    // given
    final String expectedDescription = "Plugin to automatically create Artwork";
    logger.info("Expecting plugin description: {}", expectedDescription);
    // when
    final String actualDescription = plugin.getDescription();
    logger.info("Found ArtworkCreatorPlugin description: {}", actualDescription);
    // then
    assertThat(actualDescription).isEqualTo(expectedDescription);
  }

  @Test
  @DisplayName("Validate creation of Artwork image from template, params")
  void createArtwork() throws IOException {

    // given
    final int expectedImageSize = 30_000;
    logger.info("Creating params...");
    final Collection<Param<?>> params = createTemplateParams();
    logger.info("Created params: {}", params);

    // when
    final self.me.matchday.model.Image artwork = plugin.createArtwork(Match.class, params);
    logger.info("Created artwork: {}", artwork);

    // then
    assertThat(artwork).isNotNull();
    assertThat(artwork.data().length).isGreaterThanOrEqualTo(expectedImageSize);
  }

  private @NotNull @Unmodifiable Collection<Param<?>> createTemplateParams() throws IOException {

    final Color home = new Color(0, 0, 255);
    final Color away = new Color(0, 255, 255);

    final byte[] logoImage = ResourceFileReader.readBinaryData("data/TestUploadImage.png");
    final Param<byte[]> homeTeamEmblem = new Param<>("#home-team-emblem", logoImage);
    final Param<byte[]> awayTeamEmblem = new Param<>("#away-team-emblem", logoImage);
    final Param<Color> homeTeamColor = new Param<>("#home-team-color", home);
    final Param<Color> awayTeamColor = new Param<>("#away-team-color", away);
    final Param<MediaType> type = new Param<>("#type", MediaType.IMAGE_PNG);
    return List.of(homeTeamEmblem, homeTeamColor, awayTeamEmblem, awayTeamColor, type);
  }
}
