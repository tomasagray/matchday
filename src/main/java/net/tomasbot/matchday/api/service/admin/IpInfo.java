package net.tomasbot.matchday.api.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.Data;
import net.tomasbot.matchday.util.JsonParser;

public class IpInfo extends ExternalIpService {

  private static final String SERVICE_URL = "https://ipinfo.io/json";

  IpInfo() {
    super(SERVICE_URL);
  }

  @Override
  public String getIpAddress() throws IOException {
    try (InputStream is = getIpDataStream();
        InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(in)) {

      IpAddressInfo ipAddressInfo = JsonParser.fromJson(reader, IpAddressInfo.class);
      return ipAddressInfo.getIp();
    }
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
