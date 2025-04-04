package net.tomasbot.matchday.model;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@Entity
@AllArgsConstructor
public class SanityReport {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID id;

  @OneToOne(cascade = CascadeType.ALL)
  private ArtworkSanityReport artworkSanityReport;

  @OneToOne(cascade = CascadeType.ALL)
  private VideoSanityReport videoSanityReport;

  private Timestamp timestamp;
  private boolean requiresHealing;
}
