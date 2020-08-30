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

import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.Instant;
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

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName(value = "message")
@Relation(collectionRelation = "messages")
public class MessageResource extends RepresentationModel<MessageResource> {

  private String message;
  private Instant timestamp;

  @Component
  public static class MessageResourceAssembler extends
      RepresentationModelAssemblerSupport<String, MessageResource> {

    public MessageResourceAssembler() {
      // todo: What about other controllers? Depends on FileServerController.class
      super(FileServerController.class, MessageResource.class);
    }

    @Override
    public @NotNull MessageResource toModel(@NotNull String entity) {

      final MessageResource messageResource = instantiateModel(entity);
      messageResource.setMessage(entity);
      messageResource.setTimestamp(Instant.now());
      return messageResource;
    }
  }
}
