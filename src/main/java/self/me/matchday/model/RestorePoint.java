package self.me.matchday.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import self.me.matchday.db.converter.PathConverter;
import self.me.matchday.db.converter.TimestampConverter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
public class RestorePoint {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type="uuid-char")
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
    private Integer userCount;

    public RestorePoint() {}
}
