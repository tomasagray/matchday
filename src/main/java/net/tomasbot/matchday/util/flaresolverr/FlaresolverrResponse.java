package net.tomasbot.matchday.util.flaresolverr;

import java.net.URL;
import lombok.Data;

@Data
public class FlaresolverrResponse {

  private String status;
  private String message;
  private Long startTimestamp;
  private Long endTimestamp;
  private String version;
  private Solution solution;

  @Data
  public static class Solution {
    private URL url;
    private int status;
    private String userAgent;
    private String turnstile_token;
    private String response;
    private Cookie[] cookies;
  }

  @Data
  public static class Cookie {
    private String domain;
    private Long expiry;
    private boolean httpOnly;
    private String name;
    private String path;
    private String sameSite;
    private boolean secure;
    private String value;
  }
}
