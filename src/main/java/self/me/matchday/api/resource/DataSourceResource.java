package self.me.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.DataSourceController;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.PatternKitPack;
import self.me.matchday.model.PlaintextDataSource;

import java.net.URI;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "data_source")
@Relation(collectionRelation = "data_source")
public class DataSourceResource extends RepresentationModel<DataSourceResource> {

  private UUID dataSourceId;
  private Class<?> clazz;
  private URI baseUri;
  private UUID pluginId;
  private boolean enabled;

  @JsonRootName(value = "data_source")
  @Relation(collectionRelation = "data_source")
  static class PlaintextDataSourceResource extends DataSourceResource {

    @Getter @Setter private PatternKitPack patternKitPack;

    PlaintextDataSourceResource(@NotNull DataSourceResource base) {
      this.setDataSourceId(base.dataSourceId);
      this.setClazz(base.clazz);
      this.setBaseUri(base.baseUri);
      this.setPluginId(base.pluginId);
      this.setEnabled(base.enabled);
    }
  }

  @Component
  public static class DataSourceResourceAssembler
      extends RepresentationModelAssemblerSupport<DataSource<?>, DataSourceResource> {

    DataSourceResourceAssembler() {
      super(DataSourceController.class, DataSourceResource.class);
    }

    @Override
    public @NotNull DataSourceResource toModel(@NotNull DataSource<?> entity) {

      DataSourceResource resource = instantiateModel(entity);
      resource.setDataSourceId(entity.getDataSourceId());
      resource.setClazz(entity.getClazz());
      resource.setBaseUri(entity.getBaseUri());
      resource.setPluginId(entity.getPluginId());
      resource.setEnabled(entity.isEnabled());
      resource.add(
          linkTo(methodOn(DataSourceController.class).getDataSource(entity.getDataSourceId()))
              .withSelfRel());

      if (entity instanceof PlaintextDataSource) {
        final PlaintextDataSource<?> plaintextEntity = (PlaintextDataSource<?>) entity;
        PlaintextDataSourceResource plaintextResource = new PlaintextDataSourceResource(resource);
        plaintextResource.setPatternKitPack(plaintextEntity.getPatternKitPack());
        return plaintextResource;
      }
      return resource;
    }

    @Override
    public @NotNull CollectionModel<DataSourceResource> toCollectionModel(
        @NotNull Iterable<? extends DataSource<?>> entities) {

      CollectionModel<DataSourceResource> resources = super.toCollectionModel(entities);
      StreamSupport.stream(entities.spliterator(), false)
          .map(DataSource::getPluginId)
          .findAny()
          .ifPresent(
              pluginId ->
                  resources.add(
                      linkTo(methodOn(DataSourceController.class).getDataSourcesForPlugin(pluginId))
                          .withSelfRel()));
      return resources;
    }
  }
}
