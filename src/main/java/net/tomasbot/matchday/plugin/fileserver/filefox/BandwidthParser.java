package net.tomasbot.matchday.plugin.fileserver.filefox;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

@Component
public class BandwidthParser {

  private static final String PROFILE_URL = "/profile";

  private final ConnectionManager connectionManager;
  private final FileFoxPluginProperties pluginProperties;
  private final PageEvaluator pageEvaluator;

  private URI profileUri;

  public BandwidthParser(
      ConnectionManager connectionManager,
      FileFoxPluginProperties pluginProperties,
      PageEvaluator pageEvaluator) {
    this.connectionManager = connectionManager;
    this.pluginProperties = pluginProperties;
    this.pageEvaluator = pageEvaluator;
  }

  private URI getProfileUri() {
    if (profileUri == null) {
      try {
        URL url = new URL(pluginProperties.getBaseUrl() + PROFILE_URL);
        profileUri = url.toURI();
      } catch (MalformedURLException | URISyntaxException e) {
        throw new RuntimeException(e);
      }
    }

    return profileUri;
  }

  public float getRemainingBandwidth(MultiValueMap<String, String> cookies) throws IOException {
    ClientResponse response = connectionManager.connectTo(this.getProfileUri(), cookies);
    String profileData = response.bodyToMono(String.class).block();
    FileFoxPage fileFoxPage = pageEvaluator.getFileFoxPage(profileData);

    if (fileFoxPage instanceof FileFoxPage.Profile profile) {
      return profile.getTrafficAvailable();
    }

    FileFoxParsingException cause = new FileFoxParsingException(fileFoxPage.getText());
    throw new IOException("Could not determine remaining bandwidth", cause);
  }
}
