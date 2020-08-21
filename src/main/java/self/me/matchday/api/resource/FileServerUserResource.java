package self.me.matchday.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.FileServerController;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerUser;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName(value = "user")
@Relation(collectionRelation = "users")
@JsonInclude(value = Include.NON_NULL)
public class FileServerUserResource extends RepresentationModel<FileServerUserResource> {

  private String id;
  private String username;
  private String email;
  private boolean loggedIn;
  private Collection<SecureCookie> cookies;

  @Component
  public static class UserResourceAssembler extends
      RepresentationModelAssemblerSupport<FileServerUser, FileServerUserResource> {

    public UserResourceAssembler() {
      super(FileServerController.class, FileServerUserResource.class);
    }

    @Override
    public @NotNull FileServerUserResource toModel(@NotNull final FileServerUser entity) {

      final FileServerUserResource resource = instantiateModel(entity);
      resource.setId(entity.getUserId());
      resource.setUsername(entity.getUserName());
      resource.setEmail(entity.getEmail());
      resource.setLoggedIn(entity.isLoggedIn());
      resource.setCookies(entity.getCookies());

      // Add HATEOAS self link
      resource.add(linkTo(
          methodOn(FileServerController.class)
              .getUserData(entity.getUserId()))
          .withSelfRel());

      return resource;
    }
  }
}
