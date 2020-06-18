package self.me.matchday.feed.blogger;

import java.io.IOException;
import java.net.URL;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.feed.blogger.html.BloggerHtmlBuilder;
import self.me.matchday.feed.blogger.html.HtmlPostBuilderFactory;
import self.me.matchday.feed.blogger.json.BloggerJsonBuilder;
import self.me.matchday.feed.blogger.json.JsonPostBuilderFactory;

public class BloggerFactory {

  /**
   * Creates a Blogger instance from JSON, which is read from the specified URL
   *
   * @param url The URL of the JSON representing a Blogger blog.
   * @return A Blogger instance
   * @throws IOException If the source cannot be read.
   */
  public static Blogger fromJson(@NotNull final URL url) throws IOException {

    return
        new BloggerJsonBuilder(url, new JsonPostBuilderFactory())
            .getBlogger();
  }

  /**
   * Creates a Blogger instance from HTML which is read from the specified URL.
   *
   * @param url The URL of the HTML file representing a Blogger blog.
   * @return A Blogger instance
   * @throws IOException If the HTML file cannot be read.
   */
  public static Blogger fromHtml(@NotNull final URL url) throws IOException {

    return
        new BloggerHtmlBuilder(url, new HtmlPostBuilderFactory())
            .getBlogger();
  }
}
