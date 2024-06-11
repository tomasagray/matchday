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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import lombok.*;
import net.tomasbot.matchday.api.controller.DataSourceController;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.PatternKit;
import net.tomasbot.matchday.model.PlaintextDataSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "data_source")
@Relation(collectionRelation = "data_source")
public class DataSourceResource extends RepresentationModel<DataSourceResource> {

  private UUID dataSourceId;
  private Class<?> clazz;
  private String title;
  private URI baseUri;
  private UUID pluginId;
  private boolean enabled;

  @Getter
  @Setter
  @JsonRootName(value = "data_source")
  @Relation(collectionRelation = "data_source")
  static class PlaintextDataSourceResource extends DataSourceResource {

    private List<? extends PatternKit<?>> patternKits;

    PlaintextDataSourceResource(@NotNull DataSourceResource base) {
      this.setDataSourceId(base.dataSourceId);
      this.setClazz(base.clazz);
      this.setTitle(base.title);
      this.setBaseUri(base.baseUri);
      this.setPluginId(base.pluginId);
      this.setEnabled(base.enabled);
    }
  }

  @Component
  public static class DataSourceResourceAssembler
      extends RepresentationModelAssemblerSupport<DataSource<?>, DataSourceResource> {

    DataSourceResourceAssembler() {
      super(DataSourceController.class, DataSourceResource.class);
    }

    @Override
    public @NotNull DataSourceResource toModel(@NotNull DataSource<?> entity) {

      DataSourceResource resource = instantiateModel(entity);
      resource.setDataSourceId(entity.getDataSourceId());
      resource.setClazz(entity.getClazz());
      resource.setTitle(entity.getTitle());
      resource.setBaseUri(entity.getBaseUri());
      resource.setPluginId(entity.getPluginId());
      resource.setEnabled(entity.isEnabled());
      resource.add(
          linkTo(methodOn(DataSourceController.class).getDataSource(entity.getDataSourceId()))
              .withSelfRel());

      if (entity instanceof final PlaintextDataSource<?> plaintextEntity) {
        PlaintextDataSourceResource plaintextResource = new PlaintextDataSourceResource(resource);
        final List<PatternKit<?>> patternKits = plaintextEntity.getPatternKits();
        plaintextResource.setPatternKits(patternKits);
        return plaintextResource;
      }
      return resource;
    }

    @Override
    public @NotNull CollectionModel<DataSourceResource> toCollectionModel(
        @NotNull Iterable<? extends DataSource<?>> entities) {

      CollectionModel<DataSourceResource> resources = super.toCollectionModel(entities);
      StreamSupport.stream(entities.spliterator(), false)
          .map(DataSource::getPluginId)
          .findAny()
          .ifPresent(
              pluginId ->
                  resources.add(
                      linkTo(methodOn(DataSourceController.class).getDataSourcesForPlugin(pluginId))
                          .withSelfRel()));
      return resources;
    }
  }
}
