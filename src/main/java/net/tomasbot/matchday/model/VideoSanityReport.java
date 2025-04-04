package net.tomasbot.matchday.model;

import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import net.tomasbot.matchday.model.video.TaskListState;
import net.tomasbot.matchday.model.video.TaskState;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@Entity
@AllArgsConstructor
public class VideoSanityReport {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID id;

  @OneToMany(cascade = CascadeType.ALL)
  @ToString.Exclude
  private List<DanglingLocatorPlaylist> danglingPlaylists;

  @OneToMany(cascade = CascadeType.ALL)
  @ToString.Exclude
  private List<DanglingVideoStreamLocator> danglingStreamLocators;

  private long totalLocatorPlaylists;
  private long totalStreamLocators;

  @Entity
  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  public static class DanglingLocatorPlaylist {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-char")
    private UUID id;

    private Long playlistId;
    private UUID fileSrcId;
    private String state;
    private double completionRatio;
    private String storageLocation;

    @OneToMany(cascade = CascadeType.ALL)
    private List<DanglingVideoStreamLocator> streamLocators;

    public DanglingLocatorPlaylist(@NotNull VideoStreamLocatorPlaylist playlist) {
      TaskListState state = playlist.getState();

      this.state = state.getStatus().name();
      this.completionRatio = state.getCompletionRatio();
      this.playlistId = playlist.getId();
      this.fileSrcId = playlist.getFileSource().getFileSrcId();
      this.storageLocation = playlist.getStorageLocation().toString();
      this.streamLocators =
          playlist.getStreamLocators().stream().map(DanglingVideoStreamLocator::new).toList();
    }
  }

  @Entity
  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  public static class DanglingVideoStreamLocator {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-char")
    private UUID id;

    private Long streamLocatorId;
    private String state;
    private double completionRatio;

    @Type(type = "uuid-char")
    private UUID videoFileId;

    private String playlistPath;

    public DanglingVideoStreamLocator(@NotNull VideoStreamLocator locator) {
      TaskState state = locator.getState();

      this.state = state.getStatus().name();
      this.completionRatio = state.getCompletionRatio();
      this.streamLocatorId = locator.getStreamLocatorId();
      this.videoFileId = locator.getVideoFile().getFileId();
      this.playlistPath = locator.getPlaylistPath().toString();
    }
  }
}
