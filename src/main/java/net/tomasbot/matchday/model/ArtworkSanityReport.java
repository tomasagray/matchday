package net.tomasbot.matchday.model;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import net.tomasbot.matchday.db.converter.PathConverter;
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
public class ArtworkSanityReport {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID id;

  @ElementCollection
  @ToString.Exclude
  private List<String> danglingFiles;

  @OneToMany(cascade = CascadeType.ALL)
  @ToString.Exclude
  private List<DanglingArtwork> danglingDbEntries;

  private long totalFiles;
  private long totalDbEntries;

  @Entity
  @Getter
  @Setter
  @NoArgsConstructor
  public static class DanglingArtwork {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-char")
    private UUID id;

    private Long artworkId;
    private Long filesize;
    private int height;
    private int width;
    private String filePath;
    private String createDate;

    public DanglingArtwork(@NotNull Artwork artwork) {
      this.artworkId = artwork.getId();
      this.filesize = artwork.getFileSize();
      this.height = artwork.getHeight();
      this.width = artwork.getWidth();
      this.filePath = artwork.getFile().toString();
      this.createDate = artwork.getCreated().toString();
    }
  }
}
