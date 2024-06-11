package net.tomasbot.matchday.model;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import net.tomasbot.matchday.db.converter.PathConverter;
import net.tomasbot.matchday.db.converter.TimestampConverter;

@Entity
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class RestorePoint {

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Type(type = "uuid-char")
  private UUID id;

  @Convert(converter = PathConverter.class)
  private Path backupArchive;

  @Convert(converter = TimestampConverter.class)
  private Timestamp timestamp;

  private Long filesize;
  private Integer eventCount;
  private Integer competitionCount;
  private Integer teamCount;
  private Integer dataSourceCount;
  private Integer fileServerUserCount;

  public RestorePoint() {}

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    RestorePoint that = (RestorePoint) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
