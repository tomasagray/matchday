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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import self.me.matchday.model.ArtworkTemplate;
import self.me.matchday.model.ArtworkTemplate.Coordinate;
import self.me.matchday.model.ArtworkTemplate.Layer;
import self.me.matchday.model.ArtworkTemplate.Shape;
import self.me.matchday.model.Image;
import self.me.matchday.model.Param;
import self.me.matchday.plugin.Plugin;
import self.me.matchday.util.ResourceFileReader;

@Component
public class ArtworkCreatorPlugin implements Plugin {

  private static final SchemaFactory schemaFactory =
      SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

  private final ArtworkCreatorPluginProperties properties;
  private final Map<Class<?>, ArtworkTemplate> templateRegistry = new HashMap<>();
  private final JAXBContext jaxbContext;

  public ArtworkCreatorPlugin(ArtworkCreatorPluginProperties properties) throws JAXBException {
    this.properties = properties;
    this.jaxbContext = JAXBContext.newInstance(ArtworkTemplate.class);
  }

  private static <T> Stream<T> reverseStream(@NotNull Stream<T> stream) {
    final LinkedList<T> stack = new LinkedList<>();
    stream.forEach(stack::push);
    return stack.stream();
  }

  @NotNull
  private static BufferedImage getBaseTile(@NotNull ArtworkTemplate template) {
    return new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TRANSLUCENT);
  }

  @Override
  public UUID getPluginId() {
    return UUID.fromString(properties.getId());
  }

  @Override
  public String getTitle() {
    return properties.getTitle();
  }

  @Override
  public String getDescription() {
    return properties.getDescription();
  }

  public @NotNull Image createArtwork(@NotNull Class<?> type, @NotNull Collection<Param<?>> params)
      throws IOException {

    try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      final ArtworkTemplate template = getTemplateFor(type);
      // get a mutable copy
      final ArrayList<Param<?>> _params = new ArrayList<>(params);
      // add default params
      _params.add(new Param<>("#width", template.getWidth()));
      _params.add(new Param<>("#height", template.getHeight()));

      final BufferedImage baseTile = getBaseTile(template);
      final Graphics2D graphics = baseTile.createGraphics();
      reverseStream(template.getLayer().stream())
          .map(layer -> renderLayer(layer, template, _params))
          .forEach(image -> graphics.drawImage(image, 0, 0, null));
      graphics.dispose();

      final boolean written = ImageIO.write(baseTile, "png", output);
      if (!written) {
        throw new IOException("Could not write image data: no appropriate writer found");
      }
      return new Image(output.toByteArray(), MediaType.IMAGE_PNG); // todo - get type from params
    }
  }

  private @NotNull BufferedImage renderLayer(
      @NotNull Layer layer,
      @NotNull ArtworkTemplate template,
      @NotNull Collection<Param<?>> params) {

    final BufferedImage buffer = getBaseTile(template);
    final Graphics2D graphics = (Graphics2D) buffer.getGraphics();

    final List<Shape> shapes = layer.getShape();
    if (shapes != null) {
      shapes.forEach(shape -> renderShape(params, graphics, shape));
    }
    final List<ArtworkTemplate.Image> images = layer.getImage();
    if (images != null) {
      images.forEach(image -> renderImage(params, graphics, image));
    }
    graphics.dispose();
    return buffer;
  }

  private Param<?> getMatchingParam(@NotNull Collection<Param<?>> params, @NotNull String tag) {
    return params.stream().filter(param -> param.nameMatches(tag)).findFirst().orElse(null);
  }

  private void renderShape(
      @NotNull Collection<Param<?>> params, @NotNull Graphics2D graphics, @NotNull Shape shape) {

    final Param<?> color = getMatchingParam(params, shape.getColor());
    final GeneralPath path = createPathFrom(shape);
    final Color colorData = getValidColor((Color) color.getData());
    graphics.setColor(colorData);
    graphics.setPaint(colorData);
    graphics.fill(path);
  }

  /**
   * Correct data corruption which may have been introduced during serialization/deserialization
   *
   * @param color The reconstituted color
   * @return A corrected version of the input color
   */
  @Contract("_ -> new")
  private @NotNull Color getValidColor(@NotNull Color color) {
    return new Color(color.getRed(), color.getGreen(), color.getBlue());
  }

  private void renderImage(
      @NotNull Collection<Param<?>> params,
      @NotNull Graphics2D graphics,
      ArtworkTemplate.@NotNull Image image) {
    final Param<?> data = getMatchingParam(params, image.getSrc());
    final BufferedImage bufferedImage = createBufferedImage(data);
    final Coordinate origin = image.getOrigin();
    graphics.drawImage(
        bufferedImage, origin.getX(), origin.getY(), image.getWidth(), image.getHeight(), null);
  }

  private @Nullable BufferedImage createBufferedImage(Param<?> param) {

    try {
      if (param != null && param.getData() instanceof byte[]) {
        final byte[] imgData = (byte[]) param.getData();
        return ImageIO.read(new ByteArrayInputStream(imgData));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return null;
  }

  private @Nullable GeneralPath createPathFrom(@NotNull Shape shape) {

    final List<Coordinate> coordinates = shape.getCoords();
    if (coordinates.size() > 0) {
      final GeneralPath path = new GeneralPath();
      final Coordinate start = coordinates.get(0);
      path.moveTo(start.getX(), start.getY());
      for (int i = 1; i < coordinates.size(); i++) {
        final Coordinate coordinate = coordinates.get(i);
        path.lineTo(coordinate.getX(), coordinate.getY());
      }
      path.closePath();
      return path;
    }
    return null;
  }

  private @NotNull ArtworkTemplate getTemplateFor(@NotNull Class<?> type) {

    final ArtworkTemplate template = templateRegistry.get(type);
    if (template != null) {
      return template;
    }
    // else...
    final ArtworkTemplate newTemplate = readTemplateData(type);
    templateRegistry.put(type, newTemplate);
    return newTemplate;
  }

  /**
   * Reads and rehydrates an ArtworkTemplate from disk
   *
   * @param type The data type to which the template applies
   * @return ArtworkTemplate the template instance
   * @throws IllegalArgumentException, ClassCastException if the template could not be rehydrated
   */
  private ArtworkTemplate readTemplateData(@NotNull Class<?> type) {
    try {
      // get file name
      final String typeName = type.getSimpleName().toLowerCase();
      final String templateName = String.format(properties.getTemplateNamePattern(), typeName);

      final String templateData = ResourceFileReader.readTextResource(templateName);
      final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      unmarshaller.setSchema(getSchema(templateName));
      return (ArtworkTemplate) unmarshaller.unmarshal(new StringReader(templateData));

    } catch (IOException | JAXBException | SAXException e) {
      final String msg = String.format("No Artwork creation template found for type: %s", type);
      throw new IllegalArgumentException(msg, e);
    }
  }

  private @Nullable Schema getSchema(@NotNull String templateName) throws SAXException {
    final String schemaFilename = templateName.replace(".xml", ".xsd");
    final URL schemaUrl = ResourceFileReader.getResourceUrl(schemaFilename);
    if (schemaUrl != null) {
      return schemaFactory.newSchema(schemaUrl);
    }
    return null;
  }
}
