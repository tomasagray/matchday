package net.tomasbot.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.tomasbot.matchday.api.controller.ApplicationInfoController;
import net.tomasbot.matchday.model.ApplicationInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "app_info")
public class ApplicationInfoResource extends RepresentationModel<ApplicationInfoResource> {

  private Long pid;
  private String system;
  private String version;
  private String ipAddress;

  @Component
  public static class ApplicationInfoModeller
      extends RepresentationModelAssemblerSupport<ApplicationInfo, ApplicationInfoResource> {

    public ApplicationInfoModeller() {
      super(ApplicationInfoController.class, ApplicationInfoResource.class);
    }

    @Override
    public @NotNull ApplicationInfoResource toModel(@NotNull ApplicationInfo entity) {
      ApplicationInfoResource model = instantiateModel(entity);

      model.setPid(entity.getPid());
      model.setSystem(entity.getSystem());
      model.setVersion(entity.getAppVersion());
      model.setIpAddress(entity.getIpAddress());

      return model;
    }
  }
}
