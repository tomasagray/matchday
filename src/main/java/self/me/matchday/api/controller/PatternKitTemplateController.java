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

package self.me.matchday.api.controller;

import static self.me.matchday.api.resource.PatternKitTemplateResource.TemplateResourceAssembler;

import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import self.me.matchday.api.resource.PatternKitTemplateResource;
import self.me.matchday.api.service.PatternKitTemplateService;
import self.me.matchday.model.PatternKitTemplate;

@RestController
@RequestMapping("/pattern-kit-templates")
public class PatternKitTemplateController {

  private final PatternKitTemplateService templateService;
  private final TemplateResourceAssembler templateResourceAssembler;

  PatternKitTemplateController(
      PatternKitTemplateService templateService,
      TemplateResourceAssembler templateResourceAssembler) {
    this.templateService = templateService;
    this.templateResourceAssembler = templateResourceAssembler;
  }

  @RequestMapping(
      value = "/all",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CollectionModel<PatternKitTemplateResource>> fetchAllTemplates() {
    final List<PatternKitTemplate> templates = templateService.fetchAll();
    return ResponseEntity.ok(templateResourceAssembler.toCollectionModel(templates));
  }

  @RequestMapping(
      value = "/type/{className}",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PatternKitTemplateResource> fetchTemplateByClassName(
      @PathVariable String className) {
    return templateService
        .fetchByClassName(className)
        .map(templateResourceAssembler::toModel)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
