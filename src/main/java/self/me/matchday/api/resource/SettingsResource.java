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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.SettingsController;
import self.me.matchday.model.Settings;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "settings")
@Relation(collectionRelation = "settings_collection")
public class SettingsResource extends RepresentationModel<SettingsResource> {

  private Timestamp timestamp;
  private String refreshEvents;
  private String pruneVideos;
  private int videoExpiredDays;
  private String logFilename;
  private String artworkStorageLocation;
  private String videoStorageLocation;

  @Component
  public static final class SettingsResourceModeller
      extends RepresentationModelAssemblerSupport<Settings, SettingsResource> {

    public SettingsResourceModeller() {
      super(SettingsController.class, SettingsResource.class);
    }

    @Override
    public @NotNull SettingsResource toModel(@NotNull Settings entity) {
      final SettingsResource model = instantiateModel(entity);
      model.setTimestamp(entity.getTimestamp());
      model.setRefreshEvents(entity.getRefreshEvents().getExpression());
      model.setPruneVideos(entity.getPruneVideos().getExpression());
      model.setVideoExpiredDays(entity.getVideoExpiredDays());
      model.setLogFilename(entity.getLogFilename().toString());
      model.setArtworkStorageLocation(entity.getArtworkStorageLocation().toString());
      model.setVideoStorageLocation(entity.getVideoStorageLocation().toString());
      model.add(linkTo(methodOn(SettingsController.class).getSettings()).withSelfRel());
      return model;
    }
  }
}
