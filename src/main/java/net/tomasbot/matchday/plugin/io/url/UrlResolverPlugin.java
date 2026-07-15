package net.tomasbot.matchday.plugin.io.url;

import java.io.IOException;
import java.net.URL;
import org.jetbrains.annotations.NotNull;

public interface UrlResolverPlugin {

  boolean canResolve(@NotNull URL url);

  URL resolveUrl(@NotNull URL url) throws IOException;
}
