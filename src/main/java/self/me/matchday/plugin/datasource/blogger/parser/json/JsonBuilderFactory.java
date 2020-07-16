package self.me.matchday.plugin.datasource.blogger.parser.json;

import java.net.URL;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilder;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilderFactory;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerUrlBuilder;

public class JsonBuilderFactory implements BloggerBuilderFactory {

  @Override
  public BloggerBuilder getBloggerBuilder(@NotNull URL url) {

    return
        new JsonBloggerBuilder(url, new JsonPostBuilderFactory());
  }

  @Override
  public BloggerUrlBuilder getBloggerUrlBuilder(@NotNull final String baseUrl) {

    return
        new JsonUrlBuilder(baseUrl);
  }
}