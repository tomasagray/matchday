package net.tomasbot.matchday.api.service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IpService {

  private static final int CONNECTION_TIMEOUT_MS = 10 * 1_000;
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) "
          + "Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)";
  private static final String IP_SELECTOR = "input#ip";

  @Value("${system.info.external-ip-service-url}")
  private URL ipServiceUrl;

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

  private InputStream getIpDataStream() throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) ipServiceUrl.openConnection();
    connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
    connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
    connection.setRequestMethod("GET");
    connection.setRequestProperty("User-Agent", USER_AGENT);
    connection.connect();
    return connection.getInputStream();
  }
}
