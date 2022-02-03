/*
 * Copyright (c) 2021.
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
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.FileServerController;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.SecureCookie;

import java.util.Collection;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName(value = "user")
@Relation(collectionRelation = "users")
@JsonInclude(value = Include.NON_NULL)
public class FileServerUserResource extends RepresentationModel<FileServerUserResource> {

  private UUID userId;
  private String username;
  private String email;
  private boolean loggedIn;
  private Collection<SecureCookie> cookies;

  @Component
  public static class UserResourceAssembler
      extends RepresentationModelAssemblerSupport<FileServerUser, FileServerUserResource> {

    public UserResourceAssembler() {
      super(FileServerController.class, FileServerUserResource.class);
    }

    @Override
    public @NotNull FileServerUserResource toModel(@NotNull final FileServerUser entity) {

      final FileServerUserResource resource = instantiateModel(entity);
      resource.setUserId(entity.getUserId());
      resource.setUsername(entity.getUsername());
      resource.setEmail(entity.getEmail());
      resource.setLoggedIn(entity.isLoggedIn());
      resource.setCookies(entity.getCookies());

      // Add HATEOAS self link
      resource.add(
          linkTo(methodOn(FileServerController.class).getUserData(entity.getUserId()))
              .withSelfRel());

      return resource;
    }
  }
}
