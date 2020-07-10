package self.me.matchday.plugin.datasource.blogger.parser.html;

import java.net.URL;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilder;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerBuilderFactory;
import self.me.matchday.plugin.datasource.blogger.parser.BloggerUrlBuilder;

public class HtmlBuilderFactory implements BloggerBuilderFactory {

  @Override
  public BloggerBuilder getBloggerBuilder(@NotNull URL url) {

    return
        new HtmlBloggerBuilder(url, new HtmlPostBuilderFactory());
  }

  @Override
  public BloggerUrlBuilder getBloggerUrlBuilder(@NotNull String baseUrl) {

    return
        new HtmlUrlBuilder(baseUrl);
  }
}
