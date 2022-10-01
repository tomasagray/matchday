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

package self.me.matchday.api.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Example;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.ArtworkCollectionRepository;
import self.me.matchday.db.ArtworkRepository;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.ArtworkCollection;
import self.me.matchday.model.Image;

@Service
@Transactional
@PropertySource("classpath:artwork.properties")
public class ArtworkService {

  private static final String IMAGE_CONTENT_TYPE_PATTERN = "image/(\\w+)[+\\w]*";
  private final ArtworkRepository artworkRepository;
  private final ArtworkCollectionRepository collectionRepository;

  @Value("${artwork.storage-location}")
  private String BASE_STORAGE_LOCATION;

  @Value("${artwork.min-image-dimension}")
  private int MIN_IMAGE_DIMENSION;

  @Value("${artwork.max-image-dimension}")
  private int MAX_IMAGE_DIMENSION;

  public ArtworkService(
      ArtworkRepository artworkRepository, ArtworkCollectionRepository collectionRepository) {
    this.artworkRepository = artworkRepository;
    this.collectionRepository = collectionRepository;
  }

  public Image fetchArtworkData(@NotNull Artwork artwork) throws IOException {

    final byte[] data = readArtworkImageData(artwork);
    final MediaType mediaType = MediaType.valueOf(artwork.getMediaType());
    return new Image(data, mediaType);
  }

  private byte @NotNull [] readArtworkImageData(@NotNull Artwork artwork) throws IOException {
    try (final FileInputStream fis = new FileInputStream(artwork.getFile().toFile());
        final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      fis.transferTo(os);
      return os.toByteArray();
    }
  }

  public ArtworkCollection addArtworkToCollection(
      @NotNull ArtworkCollection collection, @NotNull Image image) throws IOException {

    final Artwork artwork = createArtwork(image);
    collection.add(artwork);
    return collectionRepository.saveAndFlush(collection);
  }

  public Artwork createArtwork(@NotNull Image image) throws IOException {

    final byte[] data = image.getData();
    final MediaType type = image.getContentType();
    // parse image
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
    final BufferedImage bufferedImage = ImageIO.read(inputStream);
    // validate image
    validateImage(bufferedImage);
    final Path imageLocation = getImageLocation(type);
    final String extension = getArtworkExtension(type);
    // write data to disk
    ImageIO.write(bufferedImage, extension, imageLocation.toFile());
    final Artwork artwork =
        Artwork.builder()
            .created(LocalDateTime.now())
            .mediaType(type.toString())
            .height(bufferedImage.getHeight())
            .width(bufferedImage.getWidth())
            .fileSize((long) data.length)
            .file(imageLocation)
            .build();
    return artworkRepository.save(artwork);
  }

  private @NotNull Path getImageLocation(@NotNull MediaType type) {
    final Path storageLocation = Path.of(BASE_STORAGE_LOCATION);
    final String extension = getArtworkExtension(type);
    final String fileName = String.format("%s.%s", UUID.randomUUID(), extension);
    return storageLocation.toAbsolutePath().resolve(fileName);
  }

  private String getArtworkExtension(MediaType contentType) {
    if (contentType == null) {
      throw new InvalidArtworkException("Content type was null");
    }
    final Pattern pattern = Pattern.compile(IMAGE_CONTENT_TYPE_PATTERN);
    final Matcher matcher = pattern.matcher(contentType.toString());
    if (!matcher.find()) {
      throw new InvalidArtworkException("Unknown image type: " + contentType);
    }
    return matcher.group(1);
  }

  private void validateImage(@NotNull BufferedImage image) {
    final int height = image.getHeight();
    final int width = image.getWidth();
    if (height > MAX_IMAGE_DIMENSION
        || height < MIN_IMAGE_DIMENSION
        || width > MAX_IMAGE_DIMENSION
        || width < MIN_IMAGE_DIMENSION) {
      final String msg = String.format("Illegal image dimensions - H: %s, W: %s", height, width);
      throw new InvalidArtworkException(msg);
    }
  }

  public void repairArtworkFilePaths(@NotNull ArtworkCollection collection) {
    final Set<Artwork> artworks = collection.getCollection();
    if (artworks != null) {
      artworks.forEach(
          artwork ->
              artworkRepository
                  .findOne(Example.of(artwork))
                  .map(
                      art -> {
                        artwork.setFile(art.getFile());
                        return artwork;
                      }));
    }
  }

  public ArtworkCollection deleteArtworkFromCollection(
      @NotNull ArtworkCollection collection, @NotNull Long artworkId) throws IOException {

    final Artwork artwork = collection.getById(artworkId);
    deleteArtworkFromDisk(artwork);
    final Artwork selected = collection.getSelected();
    collection.remove(artwork);
    if (selected != null) {
      // if we are deleting selected art
      if (selected.getId().equals(artworkId)) {
        collection.setSelectedIndex(0);
      } else {
        // get index of mutated collection
        final int newSelectedIndex = collection.indexOf(selected);
        collection.setSelectedIndex(newSelectedIndex);
      }
    }
    return collectionRepository.saveAndFlush(collection);
  }

  private void deleteArtworkFromDisk(@NotNull Artwork artwork) throws IOException {
    final boolean deleted = artwork.getFile().toFile().delete();
    if (!deleted) {
      throw new IOException("Could not delete Artwork from disk: " + artwork);
    }
  }
}
