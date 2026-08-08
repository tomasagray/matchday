package net.tomasbot.matchday.common;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class HttpConnectionManagerProperties {

  @Value("${application.connection-manager.max-redirect-depth}")
  private int maxRedirects;

  @Value("${application.connection-manager.user-agent}")
  private String userAgent;
}
