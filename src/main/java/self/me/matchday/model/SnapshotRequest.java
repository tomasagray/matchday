package self.me.matchday.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@ToString
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotRequest {

  private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

  @JsonFormat(pattern = DATE_PATTERN)
  @DateTimeFormat(pattern = DATE_PATTERN)
  private  LocalDateTime endDate;
  @JsonFormat(pattern = DATE_PATTERN)
  @DateTimeFormat(pattern = DATE_PATTERN)
  private  LocalDateTime startDate;
  // Boxed fields
  private boolean fetchBodies;
  private boolean fetchImages;
  private int maxResults;
  private List<String> labels = new ArrayList<>();
  private String orderBy;
  private String pageToken;
  private String status;

}
