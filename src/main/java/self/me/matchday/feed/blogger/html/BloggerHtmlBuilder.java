package self.me.matchday.feed.blogger.html;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import self.me.matchday.feed.blogger.Blogger;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.IBloggerBuilder;
import self.me.matchday.feed.blogger.InvalidBloggerFeedException;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.util.URLQueryDecoder;

public class BloggerHtmlBuilder implements IBloggerBuilder {

  // Dependencies
  private final Document html;
  private final HtmlPostBuilderFactory postBuilderFactory;

  private final String bloggerId;
  private final String title;
  private final String version;
  private final String author;
  private final String link;
  private final Stream<BloggerPost> posts;
  private long postCount;

  public BloggerHtmlBuilder(@NotNull final URL url,
      @NotNull final HtmlPostBuilderFactory postBuilderFactory) throws IOException {

    // Read HTML from URL
    final String html = TextFileReader.readRemote(url);
    // DOM-ify HTML String
    this.html = Jsoup.parse(html);
    this.postBuilderFactory = postBuilderFactory;

    // Get Blogger fields:
    this.bloggerId = parseBloggerId();
    this.title = parseTitle();
    this.version = parseVersion();
    this.author = parseAuthor();
    this.link = parseLink();
    this.posts = parseEntries();
  }

  private String parseBloggerId() {

    // Constants
    final String BLOG_ID_KEY = "targetBlogID";
    final String INVALID_BLOG_ID = "-1";

    // Get the <noscript> element of the <head>
    final Element link = this.html.select("head noscript link").first();
    // Parse URL
    final String href = link.attr("href");
    final Map<String, List<String>> query = URLQueryDecoder.decode(href);

    // Get the blog ID from the query string
    final List<String> strings = query.get(BLOG_ID_KEY);
    if (strings != null && strings.size() > 0) {
      return strings.get(0);
    } else {
      return INVALID_BLOG_ID;
    }
  }

  private @Nullable String parseTitle() {

    final Element title = this.html.select("title").first();
    return (title != null) ? title.text() : null;
  }

  private @Nullable String parseVersion() {

    // Select correct <div> element
    final Element header = this.html.select("div.Header").first();
    return (header != null) ? header.attr("data-version") : null;
  }

  private @Nullable String parseAuthor() {

    // Get the author <span> element
    final Element author = this.html.select("span[itemprop=author]").first();
    // Get the next <span> element which should contain the author data
    final Element name = author.nextElementSiblings().select("span[itemprop=name]").first();

    return (name != null) ? name.text() : null;
  }

  private @Nullable String parseLink() {

    final Element metaUrl = this.html.select("meta[property=og:url]").first();
    return (metaUrl != null) ? metaUrl.attr("content") : null;
  }

  private Stream<BloggerPost> parseEntries() {

    // Result container
    List<BloggerPost> entries  = new ArrayList<>();
    final Elements posts = this.html.select("div.date-posts > div.post-outer");

    if (posts.isEmpty()) {
      throw new InvalidBloggerFeedException("Feed Element is empty!");
    }

    posts.forEach(element -> {
      // Create post parser
      final BloggerHtmlPostBuilder htmlPostBuilder =
          this.postBuilderFactory.createBuilder(element);
      // Parse the post & add to list
      entries.add(htmlPostBuilder.getBloggerPost());
      postCount++;
    });

    return
        entries.stream();
  }

  @Override
  public Blogger getBlogger() {
    return
        new Blogger(bloggerId, title, version, author, link, posts, postCount);
  }
}
