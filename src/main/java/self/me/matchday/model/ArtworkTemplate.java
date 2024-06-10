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

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "template")
public class ArtworkTemplate {

  private List<Layer> layer;
  private int width;
  private int height;

  @XmlElement
  public List<Layer> getLayer() {
    return layer;
  }

  @XmlAttribute
  public int getWidth() {
    return width;
  }

  @XmlAttribute
  public int getHeight() {
    return height;
  }

  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Layer {

    private String name;
    private List<Image> image;
    private List<Shape> shape;

    @XmlAttribute
    public String getName() {
      return name;
    }

    @XmlElement
    public List<Image> getImage() {
      return image;
    }

    @XmlElement
    public List<Shape> getShape() {
      return shape;
    }
  }

  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Image {

    private int width;
    private int height;
    private String src;
    private Coordinate origin;

    @XmlAttribute
    public int getWidth() {
      return width;
    }

    @XmlAttribute
    public int getHeight() {
      return height;
    }

    @XmlAttribute
    public String getSrc() {
      return src;
    }

    @XmlElement
    public Coordinate getOrigin() {
      return origin;
    }
  }

  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Shape {

    private String color;
    private List<Coordinate> coords;

    @XmlAttribute
    public String getColor() {
      return color;
    }

    @XmlElement
    public List<Coordinate> getCoords() {
      return coords;
    }
  }

  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Coordinate {

    private int x;
    private int y;

    @XmlAttribute
    public int getX() {
      return x;
    }

    @XmlAttribute
    public int getY() {
      return y;
    }
  }
}
