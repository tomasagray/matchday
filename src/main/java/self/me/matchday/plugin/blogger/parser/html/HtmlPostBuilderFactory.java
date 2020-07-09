package self.me.matchday.plugin.blogger.parser.html;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

public class HtmlPostBuilderFactory {

  /**
   * Create a <b>HtmlBloggerPostBuilder</b> from a chunk of HTML.
   *
   * @param html The Jsoup Node tree representing a BloggerPost
   * @return A HtmlBloggerPostBuilder instance.
   */
  public HtmlBloggerPostBuilder createBuilder(@NotNull final Element html) {
    return
        new HtmlBloggerPostBuilder(html);
  }
}
