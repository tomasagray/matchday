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
import static self.me.matchday.config.settings.EnabledFileServerPlugins.ENABLED_FILESERVERS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.UUID;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.FileServerPluginController;
import self.me.matchday.api.service.PluginService;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName(value = "file-server")
@Relation(collectionRelation = "fileservers")
@JsonInclude(value = Include.NON_NULL)
public class FileServerPluginResource extends RepresentationModel<FileServerPluginResource> {

  private UUID id;
  private String title;
  private String description;
  private boolean isEnabled;

  @Component
  public static class FileServerResourceAssembler
      extends RepresentationModelAssemblerSupport<FileServerPlugin, FileServerPluginResource> {

    private final PluginService pluginService;

    public FileServerResourceAssembler(PluginService pluginService) {
      super(FileServerPluginController.class, FileServerPluginResource.class);
      this.pluginService = pluginService;
    }

    @Override
    public @NotNull FileServerPluginResource toModel(@NotNull final FileServerPlugin plugin) {
      FileServerPluginResource resource = instantiateModel(plugin);
      // Add data
      resource.setId(plugin.getPluginId());
      resource.setTitle(plugin.getTitle());
      resource.setDescription(plugin.getDescription());
      boolean isEnabled = pluginService.isPluginEnabled(plugin, ENABLED_FILESERVERS);
      resource.setEnabled(isEnabled);

      // Add HATEOAS self link
      resource.add(
          linkTo(methodOn(FileServerPluginController.class).getFileServerById(plugin.getPluginId()))
              .withSelfRel());

      return resource;
    }

    @Override
    public @NotNull CollectionModel<FileServerPluginResource> toCollectionModel(
        @NotNull final Iterable<? extends FileServerPlugin> entities) {
      final CollectionModel<FileServerPluginResource> collectionModel =
          super.toCollectionModel(entities);
      // Add HATEOAS self link & return
      collectionModel.add(
          linkTo(methodOn(FileServerPluginController.class).getAllFileServerPlugins())
              .withSelfRel());
      return collectionModel;
    }
  }
}
