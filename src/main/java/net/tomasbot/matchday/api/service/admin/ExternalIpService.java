package net.tomasbot.matchday.api.service.admin;

import java.io.IOException;

public abstract class ExternalIpService {
  
  public abstract String getIpAddress() throws IOException;

  public abstract String getName();

  @Override
  public String toString() {
    return this.getName();
  }
}
