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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.resource.HighlightResource.HighlightResourceAssembler;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.model.Event;
import self.me.matchday.model.Highlight;
import self.me.matchday.model.Match;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "event")
@Relation(collectionRelation = "events")
@JsonInclude(value = Include.NON_NULL)
public class EventsResource extends RepresentationModel<EventsResource> {

  public static final LinkRelation VIDEO_LINK = LinkRelation.of("video");

  private final List<MatchResource> matches = new ArrayList<>();
  private final List<HighlightResource> highlights = new ArrayList<>();

  @Component
  public static class EventResourceAssembler
      extends RepresentationModelAssemblerSupport<List<? extends Event>, EventsResource> {

    private final MatchResourceAssembler matchAssembler;
    private final HighlightResourceAssembler highlightAssembler;

    @Autowired
    public EventResourceAssembler(
        MatchResourceAssembler matchAssembler, HighlightResourceAssembler highlightAssembler) {

      super(EventController.class, EventsResource.class);
      this.matchAssembler = matchAssembler;
      this.highlightAssembler = highlightAssembler;
    }

    @Override
    public @NotNull EventsResource toModel(@NotNull List<? extends Event> events) {

      final EventsResource eventsResource = instantiateModel(events);
      events.forEach(
          event -> {
            if (event instanceof Match) {
              eventsResource.matches.add(matchAssembler.toModel((Match) event));
            } else if (event instanceof Highlight) {
              eventsResource.highlights.add(highlightAssembler.toModel((Highlight) event));
            }
          });
      return eventsResource;
    }
  }
}
