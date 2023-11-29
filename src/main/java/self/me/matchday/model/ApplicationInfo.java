package self.me.matchday.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationInfo {
  private String version;
  private String system;
  private Long pid;
}
