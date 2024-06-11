package net.tomasbot.matchday.api.resource;

import org.jetbrains.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

public abstract class EntityModeller<T, R extends RepresentationModel<?>>
    extends RepresentationModelAssemblerSupport<T, R> {

  /**
   * Creates a new {@link RepresentationModelAssemblerSupport} using the given controller class and
   * resource type.
   *
   * @param controllerClass must not be {@literal null}.
   * @param resourceType must not be {@literal null}.
   */
  public EntityModeller(Class<?> controllerClass, Class<R> resourceType) {
    super(controllerClass, resourceType);
  }

  public abstract T fromModel(@Nullable R model);
}
