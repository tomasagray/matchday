package net.tomasbot.matchday.common.flaresolverr;

import static net.tomasbot.matchday.config.settings.FlaresolverrMaxTimeout.FLARESOLVERR_TIMEOUT;
import static net.tomasbot.matchday.config.settings.FlaresolverrUrl.FLARESOLVERR_URL;

import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.common.JsonParser;
import net.tomasbot.matchday.model.SecureCookie;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class FlaresolverrService {

  private final SettingsService settingsService;
  // TODO: convert to RestClient when upgrading to Spring Boot 3.2+
  private final WebClient webClient;

  public FlaresolverrService(SettingsService settingsService, WebClient flareWebClient) {
    this.settingsService = settingsService;
    this.webClient = flareWebClient;
  }

  private static void validateUrl(@Nullable URL url) {
    if (url == null) throw new IllegalArgumentException("URL was null");
    // TODO: more validations...
  }

  /**
   * Create an internal cookie view from a returned Flaresolverr cookie
   *
   * @param cookie The cookie returned by the Flaresolverr service
   * @return An internally comprehensible cookie
   */
  private static SecureCookie parseCookie(@NotNull FlaresolverrResponse.Cookie cookie) {
    return SecureCookie.builder()
        .name(cookie.getName())
        .cookieValue(cookie.getValue())
        .domain(cookie.getDomain())
        .httpOnly(cookie.isHttpOnly())
        .sameSite(cookie.getSameSite())
        .secure(cookie.isSecure())
        .path(cookie.getPath())
        .build();
  }

  public static @NotNull FlaresolverrSolution solutionFrom(@NotNull FlaresolverrResponse response) {
    FlaresolverrResponse.Solution solution = response.getSolution();

    URL url = solution.getUrl();
    int status = solution.getStatus();
    String responseData = solution.getResponse();
    // map cookies
    Set<SecureCookie> cookies =
        Arrays.stream(solution.getCookies())
            .map(FlaresolverrService::parseCookie)
            .collect(Collectors.toSet());

    return new FlaresolverrSolution(url, status, responseData, cookies);
  }

  private @NotNull FlaresolverrRequestData getFlaresolverrRequest(@NotNull URL url) {
    Long timeout = settingsService.getSetting(FLARESOLVERR_TIMEOUT, Long.class);
    return new FlaresolverrRequestData(url, timeout);
  }

  public FlaresolverrSolution solveChallengeFor(@NotNull URL url) {
    validateUrl(url);

    FlaresolverrRequestData requestData = getFlaresolverrRequest(url);
    String json = JsonParser.toJson(requestData, FlaresolverrRequestData.class);
    String uri = settingsService.getSetting(FLARESOLVERR_URL, URL.class).toString();

    String rawResponse =
        webClient
            .post()
            .uri(uri) // post to Flaresolverr URL
            .bodyValue(json)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMillis(requestData.getMaxTimeout()))
            .block();

    FlaresolverrResponse response = JsonParser.fromJson(rawResponse, FlaresolverrResponse.class);
    validateResponse(response);

    return solutionFrom(response);
  }

  @Contract("null -> fail")
  private static void validateResponse(FlaresolverrResponse response) {
    if (response == null) throw new FlaresolverrException("Response was null");
    FlaresolverrResponse.Solution solution = response.getSolution();
    if (solution == null)
      throw new FlaresolverrException("Could not solve challenge: null solution");
  }

  @Configuration
  public static class WebClientConfig {

    private static final int MEM_SIZE = 16 * 1024 * 1024;

    @Bean
    public WebClient flareWebClient(WebClient.Builder builder) {
      ExchangeStrategies strategies =
          ExchangeStrategies.builder()
              .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MEM_SIZE))
              .build();

      return builder
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .exchangeStrategies(strategies)
          .build();
    }
  }
}
