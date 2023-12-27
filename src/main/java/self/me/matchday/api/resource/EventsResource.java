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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.EventController;
import self.me.matchday.api.resource.HighlightResource.HighlightResourceAssembler;
import self.me.matchday.api.resource.MatchResource.MatchResourceAssembler;
import self.me.matchday.model.Event;
import self.me.matchday.model.Highlight;
import self.me.matchday.model.Match;

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
  public static class EventsModeller
      extends EntityModeller<Collection<? extends Event>, EventsResource> {

    private final MatchResourceAssembler matchAssembler;
    private final HighlightResourceAssembler highlightAssembler;

    @Autowired
    public EventsModeller(
        MatchResourceAssembler matchAssembler, HighlightResourceAssembler highlightAssembler) {

      super(EventController.class, EventsResource.class);
      this.matchAssembler = matchAssembler;
      this.highlightAssembler = highlightAssembler;
    }

    @Override
    public @NotNull EventsResource toModel(@NotNull Collection<? extends Event> events) {

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

    @Override
    public Collection<? extends Event> fromModel(@Nullable EventsResource model) {
      if (model == null) return null;
      List<? extends Event> matches =
          model.getMatches().stream().map(matchAssembler::fromModel).toList();
      List<? extends Event> highlights =
          model.getHighlights().stream().map(highlightAssembler::fromModel).toList();
      List<Event> events = new ArrayList<>(matches);
      events.addAll(highlights);
      return events;
    }
  }
}
