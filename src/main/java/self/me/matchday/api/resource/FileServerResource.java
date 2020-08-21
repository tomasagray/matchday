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
