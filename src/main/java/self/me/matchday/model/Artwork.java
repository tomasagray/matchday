package self.me.matchday.model;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;

@Entity
@Data
public class Artwork {

  @Id
  @GeneratedValue
  private final Long id;
  private final String filePath;
  private final String fileName;
  private final Long fileSize;
  private final MediaType mediaType;
  private final int width;
  private final int height;
  private final LocalDateTime created;
  private final LocalDateTime modified;

}
