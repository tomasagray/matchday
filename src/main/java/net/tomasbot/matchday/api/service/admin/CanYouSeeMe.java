package net.tomasbot.matchday.api.service.admin;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import lombok.Getter;
import net.tomasbot.matchday.common.HttpConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class CanYouSeeMe extends ExternalIpService {

  private static final URI SERVICE_URI = URI.create("https://www.canyouseeme.org");
  private static final String IP_SELECTOR = "input#ip";

  @Getter private final String name = "CanYouSeeMe.org";

  private final HttpConnectionManager connectionManager;

  public CanYouSeeMe(HttpConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  @Override
  public @NotNull String getIpAddress() throws IOException {
    String rawResponse =
        connectionManager.get(SERVICE_URI, new ArrayList<>()).bodyToMono(String.class).block();
    if (rawResponse == null || rawResponse.isBlank())
      throw new IOException("[CanYouSeeMe.org] Response was empty");

    Document document = Jsoup.parse(rawResponse);
    Elements inputs = document.select(IP_SELECTOR);
    Element first = inputs.first();
    if (first == null)
      throw new IOException("[CanYouSeeMe.org] Response did not contain an IP address");

    return first.attr("value");
  }
}
