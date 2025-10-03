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

import static net.tomasbot.matchday.util.Constants.LinkRelations.VIDEO_LINK_REL;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.tomasbot.matchday.api.controller.EventController;
import net.tomasbot.matchday.api.controller.HighlightController;
import net.tomasbot.matchday.api.resource.CompetitionResource.CompetitionModeller;
import net.tomasbot.matchday.model.Competition;
import net.tomasbot.matchday.model.Fixture;
import net.tomasbot.matchday.model.Highlight;
import net.tomasbot.matchday.model.Season;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;

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
  private CompetitionResource competition;
  private Season season;
  private Fixture fixture;
  private LocalDateTime date;

  @Component
  public static class HighlightResourceAssembler
      extends EntityModeller<Highlight, HighlightResource> {

    private final CompetitionModeller competitionModeller;

    public HighlightResourceAssembler(CompetitionModeller competitionModeller) {
      super(HighlightController.class, HighlightResource.class);
      this.competitionModeller = competitionModeller;
    }

    @Override
    public @NotNull HighlightResource toModel(@NotNull Highlight entity) {

      final HighlightResource resource = instantiateModel(entity);
      final UUID eventId = entity.getEventId();
      final CompetitionResource competitionModel =
          competitionModeller.toModel(entity.getCompetition());

      resource.setEventId(eventId);
      resource.setTitle(entity.getTitle());
      resource.setCompetition(competitionModel);
      resource.setSeason(entity.getSeason());
      resource.setFixture(entity.getFixture());
      resource.setDate(entity.getDate());
      resource.add(
          linkTo(methodOn(EventController.class).getVideoResources(eventId))
              .withRel(VIDEO_LINK_REL));
      resource.add(
          linkTo(methodOn(HighlightController.class).fetchHighlightById(eventId)).withSelfRel());
      return resource;
    }

    @Override
    public Highlight fromModel(@Nullable HighlightResource resource) {
      if (resource == null) return null;
      final Competition competition = competitionModeller.fromModel(resource.getCompetition());
      return Highlight.highlightBuilder()
          .eventId(resource.getEventId())
          .competition(competition)
          .season(resource.getSeason())
          .fixture(resource.getFixture())
          .date(resource.getDate())
          .build();
    }
  }
}
