package self.me.matchday.model;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Artwork {

  @Id
  @GeneratedValue
  private Long id;
  private String filePath;
  private String fileName;
  private Long fileSize;
  private String mediaType;
  private int width;
  private int height;
  private LocalDateTime created;
  private LocalDateTime modified;

}
