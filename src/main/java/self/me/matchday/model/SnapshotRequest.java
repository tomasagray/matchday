package self.me.matchday.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
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
  private Boolean fetchBodies;
  private Boolean fetchImages;
  private Integer maxResults;
  private String[] labels;
  private String orderBy;
  private String pageToken;
  private String status;

  // Provide safe auto-unboxing
  public int getMaxResults() {
    return (maxResults == null) ? 0 : maxResults;
  }

  public boolean isFetchImages() {
    return (fetchImages == null) ? false : fetchImages;
  }

  public boolean isFetchBodies() {
    return (fetchBodies == null) ? false : fetchBodies;
  }
}
