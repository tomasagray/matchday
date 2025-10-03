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

package net.tomasbot.matchday.api.resource;

import static net.tomasbot.matchday.util.Constants.LinkRelations.FLAG_REL;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.matchday.api.controller.CountryController;
import net.tomasbot.matchday.model.Country;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "country")
@Relation(collectionRelation = "countries")
public class CountryResource extends RepresentationModel<CountryResource> {

  private String name;
  private List<Locale> locales;

  @Component
  public static class CountryResourceAssembler
      extends RepresentationModelAssemblerSupport<Country, CountryResource> {

    public CountryResourceAssembler() {
      super(CountryController.class, CountryResource.class);
    }

    @Override
    public @NotNull CountryResource toModel(@NotNull Country entity) {
      final CountryResource resource = instantiateModel(entity);
      try {
        final String name = entity.getName();
        resource.setName(name);
        resource.setLocales(entity.getLocales());
        resource.add(
            linkTo(methodOn(CountryController.class).getFlagForCountry(name)).withRel(FLAG_REL));
        resource.add(linkTo(methodOn(CountryController.class).getCountry(name)).withSelfRel());
      } catch (IOException e) {
        // this should not happen
        throw new RuntimeException(e);
      }
      return resource;
    }
  }
}
