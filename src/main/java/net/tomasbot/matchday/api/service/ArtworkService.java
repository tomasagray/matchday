/*
 * Copyright (c) 2023.
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

package net.tomasbot.matchday.api.service;

import static net.tomasbot.matchday.config.settings.ArtworkStorageLocation.ARTWORK_LOCATION;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.tomasbot.matchday.db.ArtworkCollectionRepository;
import net.tomasbot.matchday.db.ArtworkRepository;
import net.tomasbot.matchday.model.*;
import net.tomasbot.matchday.plugin.artwork.creator.ArtworkCreatorPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@PropertySource("classpath:artwork.properties")
public class ArtworkService {

  private static final String IMAGE_CONTENT_TYPE_PATTERN = "image/(\\w+)[+\\w]*";

  private final ArtworkRepository artworkRepository;
  private final ArtworkCollectionRepository collectionRepository;
  private final ArtworkCreatorPlugin creatorPlugin;
  private final SettingsService settingsService;

  @Value("${artwork.min-image-dimension}")
  private int MIN_IMAGE_DIMENSION;

  @Value("${artwork.max-image-dimension}")
  private int MAX_IMAGE_DIMENSION;

  public ArtworkService(
      ArtworkRepository artworkRepository,
      ArtworkCollectionRepository collectionRepository,
      ArtworkCreatorPlugin creatorPlugin,
      SettingsService settingsService) {
    this.artworkRepository = artworkRepository;
    this.collectionRepository = collectionRepository;
    this.creatorPlugin = creatorPlugin;
    this.settingsService = settingsService;
  }

  public Image readArtworkData(@NotNull Artwork artwork) throws IOException {
    final byte[] data = readArtworkImageData(artwork);
    final MediaType mediaType = MediaType.valueOf(artwork.getMediaType());
    return new Image(data, mediaType);
  }

  public List<Artwork> fetchAllArtwork() {
    return artworkRepository.findAll();
  }

  public Optional<Artwork> fetchArtworkAt(@NotNull Path path) {
    return artworkRepository.findByFile(path);
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
    final byte[] data = image.data();
    final MediaType type = image.contentType();
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

  public Artwork createArtwork(@NotNull Class<?> type, @NotNull Collection<Param<?>> params)
      throws IOException {
    final Image artwork = creatorPlugin.createArtwork(type, params);
    return createArtwork(artwork);
  }

  /**
   * Given two Collections of colors, find 2 that are sufficiently visually contrasting. Defaults to
   * the 'primary color' in each list. Primary color is assumed to be the first one in the
   * collection (index 0).
   *
   * @param colors1 Primary list of colors
   * @param colors2 Secondary list of colors
   * @return A 2x1 array containing the contrasting colors, or null if no suitable pair was found
   */
  public Color[] getContrastingColorPair(List<Color> colors1, List<Color> colors2) {
    if (colors1 == null || colors1.isEmpty() || colors2 == null || colors2.isEmpty()) {
      return null;
    }

    // compare colors in list 1...
    for (final Color c1 : colors1) {
      final float l1 = getRelativeLuminance(c1);
      // ... to colors in list 2
      for (final Color c2 : colors2) {
        final float l2 = getRelativeLuminance(c2);
        final float contrast = Math.abs(l2 - l1);
        // if "contrast ratio" is sufficient
        if (contrast > 0.1f) {
          return new Color[] {c1, c2};
        }
      }
    }
    // default - return primary colors
    return new Color[] {colors1.get(0), colors2.get(0)};
  }

  private float getRelativeLuminance(@NotNull Color color) {
    final float red = getColorFactor(color.getRed());
    final float green = getColorFactor(color.getGreen());
    final float blue = getColorFactor(color.getBlue());
    return (0.2126f * red) + (0.7152f * green) + (0.0722f * blue);
  }

  private float getColorFactor(int color) {
    final float converted = color / 255f;
    if (converted <= 0.03928) {
      return converted / 12.92f;
    } else {
      return (float) Math.pow((converted + 0.055) / 1.055, 2.4f);
    }
  }

  private @NotNull Path getImageLocation(@NotNull MediaType type) {
    UUID fileId = UUID.randomUUID();
    String extension = getArtworkExtension(type);
    String fileName = String.format("%s.%s", fileId, extension);
    Path storageLocation = settingsService.getSetting(ARTWORK_LOCATION, Path.class);
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
          artwork -> {
            Optional<Artwork> artworkOptional = artworkRepository.findById(artwork.getId());
            if (artworkOptional.isPresent()) {
              Artwork existing = artworkOptional.get();
              artwork.setFile(existing.getFile());
            }
          });
    }
  }

  public ArtworkCollection deleteArtworkFromCollection(
      @NotNull ArtworkCollection collection, @NotNull Long artworkId) throws IOException {
    final Artwork artwork = collection.getById(artworkId);
    return deleteArtworkFromCollection(collection, artwork);
  }

  private @NotNull ArtworkCollection deleteArtworkFromCollection(
      @NotNull ArtworkCollection collection, Artwork artwork) throws IOException {
    deleteArtworkFromDisk(artwork);
    final Artwork selected = collection.getSelected();
    collection.remove(artwork);
    if (selected != null) {
      // if we are deleting selected art
      if (selected.getId().equals(artwork.getId())) {
        collection.setSelectedIndex(0);
      } else {
        // get index of mutated collection
        final int newSelectedIndex = collection.indexOf(selected);
        collection.setSelectedIndex(newSelectedIndex);
      }
    }
    return collectionRepository.saveAndFlush(collection);
  }

  public void deleteArtwork(@NotNull Artwork artwork) throws IOException {
    Optional<ArtworkCollection> collectionOptional =
        collectionRepository.getCollectionForArtwork(artwork);
    if (collectionOptional.isPresent()) {
      ArtworkCollection collection = collectionOptional.get();
      deleteArtworkFromCollection(collection, artwork);
    } else {
      deleteArtworkFromDisk(artwork);
    }
    artworkRepository.deleteById(artwork.getId());
  }

  public void deleteArtworkCollection(@NotNull ArtworkCollection collection) throws IOException {
    deleteArtworkCollectionFromDisk(collection);
    // only reachable if all Artwork in collection deleted successfully
    collectionRepository.delete(collection);
  }

  private void deleteArtworkCollectionFromDisk(@NotNull ArtworkCollection collection)
      throws IOException {
    final Set<Artwork> artworks = collection.getCollection();
    for (Artwork artwork : artworks) {
      deleteArtwork(artwork);
    }
  }

  public void deleteArtworkFromDisk(Artwork artwork) throws IOException {
    if (artwork == null) return;
    final File artworkFile = artwork.getFile().toFile();
    if (!artworkFile.exists()) return;
    final boolean deleted = artworkFile.delete();
    if (!deleted || artworkFile.exists()) {
      throw new IOException("Could not delete Artwork from disk: " + artwork);
    }
  }
}
