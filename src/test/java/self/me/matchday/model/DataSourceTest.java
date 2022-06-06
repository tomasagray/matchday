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

package self.me.matchday.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.plugin.datasource.parsing.TextParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation for DataSource entity")
class DataSourceTest {

  private static final Logger logger = LogManager.getLogger(DataSourceTest.class);

  private static PlaintextDataSource<Artwork> dataSource;
  private static TextParser textParser;

  @BeforeAll
  static void setup(@Autowired TextParser textParser) throws URISyntaxException {

    DataSourceTest.textParser = textParser;

    // create test data source
    final URI uri = new URI("https://galatamanhdfb.blogspot.com/");
    final PatternKit<Artwork> patternKit = new PatternKit<>(Artwork.class);
    patternKit.setPattern(
        Pattern.compile("<img[\\w=\":;%\\s-]*src=\"([\\w.:/]*)\" [\\w\\s=\":;%_-]*/?>"));
    patternKit.setFields(Map.of(1, "filePath"));
    DataSourceTest.dataSource =
        new PlaintextDataSource<>(
            "Test Plaintext DataSource", uri, Artwork.class, List.of(patternKit));
    logger.info("Created PlaintextDataSource:\n{}", dataSource);
  }

  private static Stream<Arguments> createArtworkStream() {

    final URI baseUri = DataSourceTest.dataSource.getBaseUri();
    logger.info("Getting data from: {}", baseUri);
    final String data = getDataFromUri(baseUri);
    logger.info("Got data:\n{}", data);
    assertThat(data).isNotNull().isNotEmpty();

    final List<PatternKit<? extends Artwork>> patternKits =
        dataSource.getPatternKitsFor(Artwork.class);
    final Stream<? extends Artwork> artworkStream =
        textParser.createEntityStreams(patternKits, data);
    assertThat(artworkStream).isNotNull();
    return artworkStream.map(Arguments::of);
  }

  private static @Nullable String getDataFromUri(URI baseUri) {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(baseUri.toURL().openStream()))) {

      final StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line);
      }
      return builder.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @ParameterizedTest
  @MethodSource("createArtworkStream")
  @DisplayName("Validate data retrieval from test DataSource")
  void createNewDataSource(Artwork artwork) {

    logger.info("Got Artwork: {}", artwork);
    assertThat(artwork).isNotNull();
    assertThat(artwork.getFilePath()).isNotNull().isNotEmpty();
  }
}
