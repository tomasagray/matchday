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

package net.tomasbot.matchday.api.controller;

import java.io.IOException;
import java.util.List;
import net.tomasbot.matchday.api.resource.CountryResource;
import net.tomasbot.matchday.api.resource.CountryResource.CountryResourceAssembler;
import net.tomasbot.matchday.api.service.CountryService;
import net.tomasbot.matchday.model.Country;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/countries")
public class CountryController {

  public static final String MEDIA_TYPE_SVG_IMAGE = "image/svg+xml";

  private final CountryService service;
  private final CountryResourceAssembler resourceAssembler;

  public CountryController(CountryService service, CountryResourceAssembler resourceAssembler) {
    this.service = service;
    this.resourceAssembler = resourceAssembler;
  }

  @RequestMapping(
      value = "/all",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<CountryResource>> getAllCountries() {
    final List<Country> countries = service.getAllCountries();
    return ResponseEntity.ok(resourceAssembler.toCollectionModel(countries));
  }

  @RequestMapping(
      value = "/{name}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CountryResource> getCountry(@PathVariable String name) {

    return service
        .getCountry(name)
        .map(resourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @RequestMapping(
      value = "/{name}/flag",
      method = RequestMethod.GET,
      produces = MEDIA_TYPE_SVG_IMAGE)
  public ResponseEntity<byte[]> getFlagForCountry(@PathVariable String name) throws IOException {
    final byte[] flag = service.getFlag(name);
    return ResponseEntity.ok(flag);
  }
}
