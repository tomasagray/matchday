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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.DataSourcePluginController;
import self.me.matchday.plugin.datasource.DataSourcePlugin;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "data_source_plugin")
@Relation(collectionRelation = "data_source_plugins")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DataSourcePluginResource extends RepresentationModel<DataSourcePluginResource> {

  private UUID id;
  private String title;
  private String description;
  private boolean enabled = true;

  @Component
  public static class DataSourcePluginResourceAssembler
      extends RepresentationModelAssemblerSupport<DataSourcePlugin, DataSourcePluginResource> {

    DataSourcePluginResourceAssembler() {
      super(DataSourcePluginController.class, DataSourcePluginResource.class);
    }

    @Override
    public @NotNull DataSourcePluginResource toModel(@NotNull final DataSourcePlugin plugin) {

      final DataSourcePluginResource pluginResource = instantiateModel(plugin);
      pluginResource.setId(plugin.getPluginId());
      pluginResource.setTitle(plugin.getTitle());
      pluginResource.setDescription(plugin.getDescription());
      pluginResource.setEnabled(plugin.isEnabled());

      // Attach self link
      pluginResource.add(
          linkTo(methodOn(DataSourcePluginController.class).getAllPlugins()).withSelfRel());

      return pluginResource;
    }

    @Override
    public @NotNull CollectionModel<DataSourcePluginResource> toCollectionModel(
        @NotNull Iterable<? extends DataSourcePlugin> plugins) {

      final CollectionModel<DataSourcePluginResource> pluginResources =
          super.toCollectionModel(plugins);
      // Add self link
      pluginResources.add(
          linkTo(methodOn(DataSourcePluginController.class).getAllPlugins()).withSelfRel());
      return pluginResources;
    }
  }
}
