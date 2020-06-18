package self.me.matchday.feed.blogger.html;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

public class HtmlPostBuilderFactory {

  /**
   * Create a <b>BloggerHtmlPostBuilder</b> from a chunk of HTML.
   *
   * @param html The Jsoup Node tree representing a BloggerPost
   * @return A BloggerHtmlPostBuilder instance.
   */
  public BloggerHtmlPostBuilder createBuilder(@NotNull final Element html) {
    return
        new BloggerHtmlPostBuilder(html);
  }
}
