package net.tomasbot.matchday.api.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class CanYouSeeMe extends ExternalIpService {

  private static final String SERVICE_URL = "https://www.canyouseeme.org";
  private static final String IP_SELECTOR = "input#ip";

  CanYouSeeMe() {
    super(SERVICE_URL);
  }

  @Override
  public @NotNull String getIpAddress() throws IOException {
    try (InputStream is = getIpDataStream();
        InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(in)) {

      final String data = reader.lines().collect(Collectors.joining());
      final Document document = Jsoup.parse(data);
      final Elements input = document.select(IP_SELECTOR);

      return input.attr("value");
    }
  }
}
