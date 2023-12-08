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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static self.me.matchday.api.resource.EventsResource.VIDEO_LINK;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.controller.HighlightController;
import self.me.matchday.model.Fixture;
import self.me.matchday.model.Highlight;
import self.me.matchday.model.Season;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "highlights")
@Relation(collectionRelation = "highlights")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HighlightResource extends RepresentationModel<HighlightResource> {

  private UUID eventId;
  private String title;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;

  @Component
  public static class HighlightResourceAssembler
      extends RepresentationModelAssemblerSupport<Highlight, HighlightResource> {

    public HighlightResourceAssembler() {
      super(HighlightController.class, HighlightResource.class);
    }

    @Override
    public @NotNull HighlightResource toModel(@NotNull Highlight entity) {

      final HighlightResource resource = instantiateModel(entity);
      final UUID eventId = entity.getEventId();
      resource.setEventId(eventId);
      resource.setTitle(entity.getTitle());
      resource.setSeason(entity.getSeason());
      resource.setFixture(entity.getFixture());
      resource.setDate(entity.getDate());
      resource.add(
          linkTo(methodOn(EventController.class).getVideoResources(eventId)).withRel(VIDEO_LINK));
      resource.add(
          linkTo(methodOn(HighlightController.class).fetchHighlightById(eventId)).withSelfRel());
      return resource;
    }
  }
}
