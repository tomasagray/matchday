package net.tomasbot.matchday.api.service.admin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.Getter;

public abstract class ExternalIpService {

  private static final int CONNECTION_TIMEOUT_MS = 10 * 1_000;
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) "
          + "Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)";

  @Getter private final URL ipServiceUrl;

  ExternalIpService(String ipServiceUrl) {
    try {
      this.ipServiceUrl = new URL(ipServiceUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public abstract String getIpAddress() throws IOException;

  InputStream getIpDataStream() throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) ipServiceUrl.openConnection();

    connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
    connection.setReadTimeout(CONNECTION_TIMEOUT_MS);
    connection.setRequestMethod("GET");
    connection.setRequestProperty("User-Agent", USER_AGENT);

    connection.connect();
    return connection.getInputStream();
  }

  abstract String getName();

  @Override
  public String toString() {
    return this.getName();
  }
}
