package self.me.matchday.api.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.model.EventFileSource;
import self.me.matchday.model.EventFileSource.Resolution;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video-resource")
@Relation(collectionRelation = "video-resources", itemRelation = "video-resource")
@JsonInclude(value = Include.NON_NULL)
public class VideoResource extends RepresentationModel<VideoResource> {

  private static final LinkRelation MASTER_PLAYLIST = LinkRelation.of("direct_master");
  private static final LinkRelation DIRECT_STREAM = LinkRelation.of("direct_stream");
  private static final LinkRelation TRANSCODE_STREAM = LinkRelation.of("transcode_stream");

  private String channel;
  private String source;
  private List<String> languages;
  private String resolution;
  private String mediaContainer;
  private Long bitrate;
  private int frameRate;
  private String videoCodec;
  private String audioCodec;

  @Component
  public static class VideoResourceAssembler extends
      RepresentationModelAssemblerSupport<EventFileSource, VideoResource> {

    // TODO: Not this; brittle
    @Getter
    @Setter
    private String eventId;

    public VideoResourceAssembler() {
      super(VideoStreamingController.class, VideoResource.class);
    }

    @Override
    public @NotNull VideoResource toModel(@NotNull EventFileSource entity) {

      final VideoResource videoResource = instantiateModel(entity);

      // Nullables
      final List<String> languages = entity.getLanguages();
      final Resolution resolution = entity.getResolution();

      // Add metadata
      videoResource.channel = entity.getChannel();
      videoResource.source = entity.getSource();
      videoResource.languages = (languages.size() > 0) ? languages : null;
      videoResource.resolution = (resolution != null) ? resolution.toString() : null;
      videoResource.mediaContainer = entity.getMediaContainer();
      videoResource.bitrate = entity.getBitrate();
      videoResource.frameRate = entity.getFrameRate();
      videoResource.videoCodec = entity.getVideoCodec();
      videoResource.audioCodec = entity.getAudioCodec();

      // Add link to remote stream (no transcoding)
      videoResource.add(linkTo(
          methodOn(VideoStreamingController.class)
              .getVariantPlaylist(eventId, entity.getEventFileSrcId()))
          .withRel(DIRECT_STREAM));

      // Add link to local stream (transcoded to local disk)
      videoResource.add(linkTo(
          methodOn(VideoStreamingController.class)
              .getStreamPlaylist(eventId, entity.getEventFileSrcId()))
          .withRel(TRANSCODE_STREAM));

      return videoResource;
    }

    @Override
    public @NotNull CollectionModel<VideoResource> toCollectionModel(
        @NotNull Iterable<? extends EventFileSource> entities) {

      final CollectionModel<VideoResource> videoResources = super.toCollectionModel(entities);

      // Add link to master playlist
      videoResources.add(linkTo(
          methodOn(VideoStreamingController.class)
              .getMasterPlaylist(eventId))
          .withRel(MASTER_PLAYLIST));
      return videoResources;
    }
  }
}
