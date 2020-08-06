package self.me.matchday.model;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.UUID;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import self.me.matchday.db.converter.PathConverter;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoStreamPlaylistLocator {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Embeddable
  public static class VideoStreamPlaylistId implements Serializable {
    private String eventId;
    private UUID fileSrcId;
  }

  @EmbeddedId
  private VideoStreamPlaylistId playlistId;
  @Convert(converter = PathConverter.class)
  private Path playlistPath;

}
