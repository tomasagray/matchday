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
