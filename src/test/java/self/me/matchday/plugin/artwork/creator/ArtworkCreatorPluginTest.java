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

package self.me.matchday.plugin.artwork.creator;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.ArtworkTemplate;
import self.me.matchday.model.ArtworkTemplate.Coordinate;
import self.me.matchday.model.ArtworkTemplate.Image;
import self.me.matchday.model.ArtworkTemplate.Layer;
import self.me.matchday.model.ArtworkTemplate.Shape;
import self.me.matchday.model.Match;
import self.me.matchday.model.Param;
import self.me.matchday.util.ResourceFileReader;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Testing & validation for Artwork creation plugin")
class ArtworkCreatorPluginTest {

  private static final Logger logger = LogManager.getLogger(ArtworkCreatorPluginTest.class);
  private final ArtworkCreatorPlugin plugin;

  @Value("${artwork.data-storage-location}")
  private String dataLocation;

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
  @DisplayName("Validate reading of template from disk")
  void readTemplate() {
    // given
    final Class<Match> type = Match.class;
    logger.info("Getting Artwork creation template for: {}", type);
    // when
    final ArtworkTemplate template = plugin.getTemplateFor(type);
    logger.info("Found template: {}", template);
    // then
    assertThat(template).isNotNull();
  }

  @Test
  @Disabled
  void createXmlFromObj() throws JAXBException {
    final ArtworkTemplate template =
        new ArtworkTemplate(
            List.of(
                new Layer(
                    "team-emblems",
                    List.of(
                        new Image(400, 400, "#home-team-emblem", new Coordinate(200, 250)),
                        new Image(400, 400, "#away-team-emblem", new Coordinate(1000, 250))),
                    null),
                new Layer(
                    "home-team-color",
                    null,
                    List.of(
                        new Shape(
                            "#home-team-color",
                            List.of(
                                new Coordinate(0, 0),
                                new Coordinate(1160, 0),
                                new Coordinate(440, 900),
                                new Coordinate(0, 900))))),
                new Layer(
                    "away-team-color",
                    null,
                    List.of(
                        new Shape(
                            "#away-team-color",
                            List.of(
                                new Coordinate(0, 0),
                                new Coordinate(0, 900),
                                new Coordinate(1600, 900),
                                new Coordinate(1600, 0)))))),
            1600,
            900);

    JAXBContext context = JAXBContext.newInstance(ArtworkTemplate.class);
    Marshaller marshaller = context.createMarshaller();
    final String pathname = dataLocation + "match.artwork-template.xml";
    logger.info("Writing template data to: {}", pathname);
    marshaller.marshal(template, new File(pathname));
  }

  @Test
  @DisplayName("Validate creation of Artwork image from template, params")
  void createArtwork() throws IOException {

    logger.info("Creating params...");
    final Collection<Param<?>> params = createTemplateParams();
    logger.info("Created params: {}", params);
    final self.me.matchday.model.Image artwork = plugin.createArtwork(Match.class, params);
    logger.info("Created artwork: {}", artwork);
    assertThat(artwork).isNotNull();

    writeImageToDisk(artwork);
  }

  private void writeImageToDisk(self.me.matchday.model.@NotNull Image image) throws IOException {

    final String pathname = dataLocation + "image.png";
    final byte[] data = image.getData();
    logger.info("Writing {} bytes created image data to: {}", data.length, pathname);
    ImageIO.write(ImageIO.read(new ByteArrayInputStream(data)), "png", new File(pathname));
  }

  private @NotNull @Unmodifiable Collection<Param<?>> createTemplateParams() throws IOException {

    // needed params:
    // - home-team-emblem + dimensions
    final byte[] logoImage = ResourceFileReader.readBinaryData("data/TestUploadImage.png");
    final Param<byte[]> homeTeamEmblem = new Param<>("#home-team-emblem", logoImage);
    // - away-team-emblem + dimensions
    final Param<byte[]> awayTeamEmblem = new Param<>("#away-team-emblem", logoImage);
    // - home-team-color
    final Param<Color> homeTeamColor = new Param<>("#home-team-color", Color.BLUE);
    // - away-team-color
    final Param<Color> awayTeamColor = new Param<>("#away-team-color", Color.YELLOW);
    // - height
    final Param<Integer> height = new Param<>("#height", 900);
    // - width
    final Param<Integer> width = new Param<>("#width", 1600);
    // - image type; todo
    return List.of(homeTeamEmblem, homeTeamColor, awayTeamEmblem, awayTeamColor, height, width);
  }
}
