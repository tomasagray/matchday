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

package self.me.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.ArtworkController;
import self.me.matchday.model.Color;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "color")
@Relation(collectionRelation = "colors")
public class ColorResource extends RepresentationModel<ColorResource> {

  private Rgb rgb;
  private Hsl hsl;
  private Hsv hsv;
  private String hex;

  @Data
  @AllArgsConstructor
  static class Rgb {
    private int r;
    private int g;
    private int b;
    private int a;

    Rgb(@NotNull Color color) {
      this.r = color.getRed();
      this.g = color.getGreen();
      this.b = color.getBlue();
      this.a = color.getAlpha();
    }
  }

  @Data
  @AllArgsConstructor
  static class Hsl {
    private double h;
    private int s;
    private int l;
    private int a;

    Hsl(@NotNull Color color) {
      computeHsl(color.getRed(), color.getGreen(), color.getBlue());
    }

    private void computeHsl(int r, int g, int b) {
      final double cMax = Math.max(r, Math.max(g, b));
      final double cMin = cMax - Math.min(r, Math.max(g, b));
      this.h = computeH(r, g, b, cMax, cMin);
      this.s = computeS(cMax, cMin);
      this.l = computeL(cMax);
      this.a = 1;
    }

    private double computeH(int r, int g, int b, double cMax, double cMin) {
      double h = 0;
      if (cMax != 0) {
        if (cMax == r) {
          h = (g - b) / cMin;
        } else if (cMax == g) {
          h = (g - r) / cMin + 2d;
        } else {
          h = (r - g) / cMin + 4d;
        }
        h = Math.min(Math.round(h * 60d), 360d);
        if (h < 0d) {
          h += 360d;
        }
      }
      return h;
    }

    private int computeS(double cMax, double cMin) {
      if (cMax != 0) {
        return (int) Math.round(cMin * 100d / cMax);
      }
      return 0;
    }

    private int computeL(double cMax) {
      return (int) Math.round(cMax * 100d / 255d);
    }
  }

  @Data
  @AllArgsConstructor
  static class Hsv {
    private double h;
    private double s;
    private double v;
    private int a;

    Hsv(@NotNull Color color) {
      computeHsv(color);
    }

    private void computeHsv(@NotNull Color color) {
      final int r = color.getRed();
      final int g = color.getGreen();
      final int b = color.getBlue();

      double cMax = Math.max(r, Math.max(g, b)); // maximum of r, g, b
      double cMin = Math.min(r, Math.min(g, b)); // minimum of r, g, b
      double diff = cMax - cMin; // diff of cMax and cMin.
      this.h = computeH(r, g, b, cMax, cMin, diff);
      this.s = computeS(cMax, diff);
      this.v = cMax * 100;
      this.a = 1;
    }

    private double computeH(int r, int g, int b, double cMax, double cMin, double diff) {
      if (cMax == cMin) {
        return 0;
      } else if (cMax == r) {
        return (60 * ((g - b) / diff) + 360) % 360;
      } else if (cMax == g) {
        return (60 * ((b - r) / diff) + 120) % 360;
      } else if (cMax == b) {
        return (60 * ((r - g) / diff) + 240) % 360;
      }
      return -1;
    }

    private double computeS(double cMax, double diff) {
      if (cMax == 0) {
        return 0;
      }
      return (diff / cMax) * 100;
    }
  }

  @Component
  public static class ColorResourceModeller extends EntityModeller<Color, ColorResource> {

    public ColorResourceModeller() {
      super(ArtworkController.class, ColorResource.class);
    }

    @Override
    public @NotNull ColorResource toModel(@NotNull Color entity) {
      final ColorResource resource = instantiateModel(entity);
      resource.setRgb(new Rgb(entity));
      resource.setHsv(new Hsv(entity));
      resource.setHsl(new Hsl(entity));
      resource.setHex(getHexString(entity));
      return resource;
    }

    private String getHexString(@NotNull Color color) {
      final String red = getColorHexValue(color.getRed());
      final String green = getColorHexValue(color.getGreen());
      final String blue = getColorHexValue(color.getBlue());
      return String.format("#%s%s%s", red, green, blue);
    }

    @Contract(pure = true)
    private @NotNull String getColorHexValue(int color) {
      if (color == 0) {
        return "00";
      }
      return Integer.toHexString(color);
    }

    @Override
    public Color fromModel(@Nullable ColorResource resource) {
      if (resource == null) return null;
      final Rgb rgb = resource.getRgb();
      return new Color(rgb.getR(), rgb.getG(), rgb.getB(), rgb.getA());
    }
  }
}
