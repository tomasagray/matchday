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

package self.me.matchday.unit.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.ArtworkCollection;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Validation for ArtworkCollection model class")
public class ArtworkCollectionTest {

  private static final Logger logger = LogManager.getLogger(ArtworkCollectionTest.class);

  @Test
  @DisplayName("Ensure ArtworkCollection.setSelected correctly sets selected Artwork")
  void testSetSelectedArtwork() {
    // given
    final int expectedIndex = 2;
    final Artwork artwork1 = getArtwork();
    final Artwork artwork2 = getArtwork();
    final Artwork artwork3 = getArtwork();
    final ArtworkCollection collection = new ArtworkCollection();
    logger.info("Created empty ArtworkCollection");

    // when
    collection.addAll(List.of(artwork2, artwork3));
    collection.setSelected(artwork1);
    logger.info("ArtworkCollection is now: {}", collection);

    // then
    assertThat(collection.getSelected()).isEqualTo(artwork1);
    assertThat(collection.getSelectedIndex()).isEqualTo(expectedIndex);
  }

  private @NotNull Artwork getArtwork() {
    final Artwork artwork = new Artwork();
    artwork.setId((long) (Math.random() * 10_000));
    artwork.setMediaType("image/png");
    artwork.setFile(Path.of("test/image.png"));
    artwork.setFileSize((long) (Math.random() * 10_000));
    artwork.setHeight(400);
    artwork.setWidth(400);
    artwork.setCreated(LocalDate.now().atStartOfDay());
    artwork.setModified(LocalDateTime.now());
    return artwork;
  }

  @Test
  @DisplayName("Verify calling setSelected() on already added element works as expected")
  void testSetAlreadySelected() {
    // given
    final int expectedIndexAfterCall = 1;
    final Artwork artwork1 = getArtwork();
    final Artwork artwork2 = getArtwork();
    final Artwork artwork3 = getArtwork();
    final ArtworkCollection collection = new ArtworkCollection();
    collection.addAll(List.of(artwork1, artwork2, artwork3));
    logger.info("Created ArtworkCollection: {}", collection);

    // when
    final Artwork selectedBeforeCall = collection.getSelected();
    final int selectedIndexBeforeCall = collection.getSelectedIndex();
    logger.info(
        "Before calling setSelected, Selected is: [{}] {}",
        selectedIndexBeforeCall,
        selectedBeforeCall);
    final boolean added = collection.setSelected(artwork2);
    logger.info("Added already selected element? {}", added);
    logger.info("After calling setSelected(), ArtworkCollection is: {}", collection);

    // then
    assertThat(collection.getSelected()).isEqualTo(artwork2);
    assertThat(collection.getSelectedIndex()).isEqualTo(expectedIndexAfterCall);
  }
}
