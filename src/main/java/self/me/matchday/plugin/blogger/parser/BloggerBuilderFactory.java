package self.me.matchday.plugin.blogger.parser;

import java.net.URL;
import org.jetbrains.annotations.NotNull;

public interface BloggerBuilderFactory {

  BloggerBuilder getBloggerBuilder(@NotNull final URL url);

  BloggerUrlBuilder getBloggerUrlBuilder(@NotNull final String baseUrl);

}
