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

package self.me.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.PatternKitTemplateController;
import self.me.matchday.model.PatternKitTemplate;
import self.me.matchday.model.PatternKitTemplate.Field;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonRootName(value = "template")
@Relation(collectionRelation = "templates")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PatternKitTemplateResource extends RepresentationModel<PatternKitTemplateResource> {

  private final List<Field> fields = new ArrayList<>();
  private final List<PatternKitTemplate> relatedTemplates = new ArrayList<>();
  private Long id;
  private Class<?> type;
  private String name;

  @Component
  public static class TemplateResourceAssembler
      extends RepresentationModelAssemblerSupport<PatternKitTemplate, PatternKitTemplateResource> {

    public TemplateResourceAssembler() {
      super(PatternKitTemplateController.class, PatternKitTemplateResource.class);
    }

    @Override
    public @NotNull PatternKitTemplateResource toModel(@NotNull PatternKitTemplate entity) {

      final PatternKitTemplateResource resource = instantiateModel(entity);
      resource.setId(entity.getId());
      resource.setType(entity.getType());
      resource.setName(entity.getName());
      resource.relatedTemplates.addAll(entity.getRelatedTemplates());
      resource.fields.addAll(entity.getFields());
      resource.add(
          linkTo(
                  methodOn(PatternKitTemplateController.class)
                      .fetchTemplateByClassName(entity.getType().getName()))
              .withSelfRel());
      return resource;
    }
  }
}
