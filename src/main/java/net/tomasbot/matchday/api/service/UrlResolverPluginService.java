package net.tomasbot.matchday.api.service;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import net.tomasbot.matchday.plugin.io.url.UrlResolverPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class UrlResolverPluginService {

  private final List<UrlResolverPlugin> urlResolverPlugins;

  public UrlResolverPluginService(List<UrlResolverPlugin> urlResolverPlugins) {
    this.urlResolverPlugins = urlResolverPlugins;
  }

  public URL resolve(@NotNull URL url) throws IOException {
    for (UrlResolverPlugin plugin : urlResolverPlugins) {
      if (plugin.canResolve(url)) return plugin.resolveUrl(url);
    }

    // default: return original URL
    return url;
  }
}
