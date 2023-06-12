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

  public void setLayer(List<Layer> layer) {
    this.layer = layer;
  }

  @XmlAttribute
  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  @XmlAttribute
  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

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

    public void setName(String name) {
      this.name = name;
    }

    @XmlElement
    public List<Image> getImage() {
      return image;
    }

    public void setImage(List<Image> image) {
      this.image = image;
    }

    @XmlElement
    public List<Shape> getShape() {
      return shape;
    }

    public void setShape(List<Shape> shape) {
      this.shape = shape;
    }
  }

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

    public void setWidth(int width) {
      this.width = width;
    }

    @XmlAttribute
    public int getHeight() {
      return height;
    }

    public void setHeight(int height) {
      this.height = height;
    }

    @XmlAttribute
    public String getSrc() {
      return src;
    }

    public void setSrc(String src) {
      this.src = src;
    }

    @XmlElement
    public Coordinate getOrigin() {
      return origin;
    }

    public void setOrigin(Coordinate origin) {
      this.origin = origin;
    }
  }

  @AllArgsConstructor
  @NoArgsConstructor
  public static class Shape {

    private String color;
    private List<Coordinate> coords;

    @XmlAttribute
    public String getColor() {
      return color;
    }

    public void setColor(String color) {
      this.color = color;
    }

    @XmlElement
    public List<Coordinate> getCoords() {
      return coords;
    }

    public void setCoords(List<Coordinate> coords) {
      this.coords = coords;
    }
  }

  @AllArgsConstructor
  @NoArgsConstructor
  public static class Coordinate {

    private int x;
    private int y;

    @XmlAttribute
    public int getX() {
      return x;
    }

    public void setX(int x) {
      this.x = x;
    }

    @XmlAttribute
    public int getY() {
      return y;
    }

    public void setY(int y) {
      this.y = y;
    }
  }
}
