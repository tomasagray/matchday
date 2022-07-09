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
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.FileServerPluginController;
import self.me.matchday.api.controller.FileServerUserController;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.model.SecureCookie;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

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
  private boolean hasPassword;
  private boolean loggedIn;
  private Collection<NetscapeCookie> cookies;

  @Component
  public static class UserResourceAssembler
      extends RepresentationModelAssemblerSupport<FileServerUser, FileServerUserResource> {

    public UserResourceAssembler() {
      super(FileServerPluginController.class, FileServerUserResource.class);
    }

    @Override
    public @NotNull FileServerUserResource toModel(@NotNull final FileServerUser entity) {

      final FileServerUserResource resource = instantiateModel(entity);
      resource.setUserId(entity.getUserId());
      resource.setUsername(entity.getUsername());
      resource.setEmail(entity.getEmail());
      resource.setLoggedIn(entity.isLoggedIn());
      resource.setHasPassword(!("".equals(entity.getPassword())));
      resource.setCookies(getNetscapeCookies(entity.getCookies()));

      // Add HATEOAS self link
      resource.add(
          linkTo(methodOn(FileServerUserController.class).getUserData(entity.getUserId()))
              .withSelfRel());
      return resource;
    }

    private Collection<NetscapeCookie> getNetscapeCookies(
        @NotNull Collection<SecureCookie> cookies) {
      return cookies.stream().map(NetscapeCookie::new).collect(Collectors.toList());
    }
  }

  @Data
  static class NetscapeCookie {
    private final Long id;
    private final String domain;
    private final boolean secure;
    private final String path;
    private final String sameSite;
    private final boolean httpOnly;
    private final long maxAge;
    private final String name;
    private final String value;

    NetscapeCookie(@NotNull SecureCookie cookie) {
      this.id = cookie.getId();
      this.name = cookie.getName();
      this.value = cookie.getValue();
      final Duration maxAge = cookie.getMaxAge();
      this.maxAge = maxAge != null ? maxAge.toSeconds() : 0;
      this.domain = cookie.getDomain();
      this.path = cookie.getPath();
      this.secure = cookie.isSecure();
      this.httpOnly = cookie.isHttpOnly();
      this.sameSite = cookie.getSameSite();
    }
  }
}
