package net.tomasbot.matchday.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationInfo {
  private Long pid;
  private String system;
  private String appVersion;
  private String ipAddress;
}
