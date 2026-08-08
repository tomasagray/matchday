package net.tomasbot.matchday.api.service.admin;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import lombok.Data;
import lombok.Getter;
import net.tomasbot.matchday.common.HttpConnectionManager;
import net.tomasbot.matchday.common.JsonParser;
import org.springframework.stereotype.Service;

@Service
public class IpInfoDotIo extends ExternalIpService {

  private static final URI SERVICE_URI = URI.create("https://ipinfo.io/json");

  private final HttpConnectionManager connectionManager;

  @Getter private final String name = "ipinfo.io";

  IpInfoDotIo(HttpConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Override
  public String getIpAddress() throws IOException {
    String json =
        connectionManager.get(SERVICE_URI, new ArrayList<>()).bodyToMono(String.class).block();
    IpAddressInfo addressInfo = JsonParser.fromJson(json, IpAddressInfo.class);
    return addressInfo.getIp();
  }

  @Data
  private static class IpAddressInfo {
    private String ip;
    private String hostname;
    private String city;
    private String region;
    private String country;
    private String loc;
    private String org;
    private String postal;
    private String timezone;
    private URL readme;
  }
}
