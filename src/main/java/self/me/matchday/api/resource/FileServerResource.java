/*
 * Copyright (c) 2020. 
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
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.FileServerController;
import self.me.matchday.plugin.fileserver.FileServerPlugin;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName(value = "fileserver")
@Relation(collectionRelation = "fileservers")
@JsonInclude(value = Include.NON_NULL)
public class FileServerResource extends RepresentationModel<FileServerResource> {

  private UUID id;
  private String title;
  private String description;

  @Component
  public static class FileServerResourceAssembler extends
      RepresentationModelAssemblerSupport<FileServerPlugin, FileServerResource> {

    public FileServerResourceAssembler() {
      super(FileServerController.class, FileServerResource.class);
    }

    @Override
    public @NotNull FileServerResource toModel(@NotNull final FileServerPlugin entity) {

      FileServerResource resource = instantiateModel(entity);
      // Add data
      resource.setId(entity.getPluginId());
      resource.setTitle(entity.getTitle());
      resource.setDescription(entity.getDescription());

      // Add HATEOAS self link
      resource.add(linkTo(
          methodOn(FileServerController.class)
              .getFileServerById(entity.getPluginId()))
          .withSelfRel());

      return resource;
    }

    @Override
    public @NotNull CollectionModel<FileServerResource> toCollectionModel(
        @NotNull final Iterable<? extends FileServerPlugin> entities) {

      final CollectionModel<FileServerResource> collectionModel =
          super.toCollectionModel(entities);

      // Add HATEOAS self link & return
      collectionModel
          .add(linkTo(
              methodOn(FileServerController.class)
                  .getAllFileServers())
              .withSelfRel());
      return collectionModel;
    }
  }
}
