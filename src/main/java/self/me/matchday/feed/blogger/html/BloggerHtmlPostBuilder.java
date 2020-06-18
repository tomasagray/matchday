package self.me.matchday.feed.blogger.html;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import self.me.matchday.feed.blogger.BloggerPost;
import self.me.matchday.feed.blogger.IBloggerPostBuilder;
import self.me.matchday.feed.blogger.InvalidBloggerPostException;

public class BloggerHtmlPostBuilder implements IBloggerPostBuilder {

  private final Element html;
  // Fields
  private final String bloggerPostID;
  private final String title;
  private final String content;
  private final String link;
  private final LocalDateTime published;
  private final LocalDateTime lastUpdated;
  private final List<String> categories;

  BloggerHtmlPostBuilder(@NotNull final Element html) {

    this.html = html;
    // Parse data
    this.bloggerPostID = parsePostId();
    this.title = parseTitle();
    this.content = parseContent();
    this.link = parseLink();
    this.published = parsePublished();
    this.lastUpdated = parseLastUpdated();
    this.categories = parseCategories();
  }

  private String parsePostId() {

    // Get the post ID
    final Element metaTag = this.html.select("meta[itemprop=postId]").first();
    // Ensure we have an ID - cannot proceed otherwise!
    if (metaTag == null) {
      throw new InvalidBloggerPostException("Could not determine post ID");
    }
    return
        metaTag.attr("content");
  }

  private @Nullable String parseTitle() {

    final Element postTitle = this.html.select("h3.post-title").first();
    return
        (postTitle == null) ? null : postTitle.text();
  }

  private String parseContent() {

    final Element postBody = this.html.select("div.post-body").first();
    // Ensure we have content
    if (postBody == null) {
      throw new InvalidBloggerPostException("No post content");
    }

    return
        postBody.html();
  }

  private String parseLink() {

    // Get the post link from the post header
    final Element postLink = this.html.select("h3.post-title > a").first();
    // Ensure link was parsed
    if (postLink == null) {
      throw new InvalidBloggerPostException("Could not parse post link");
    }
    return
        postLink.attr("href");
  }

  private @Nullable LocalDateTime parsePublished() {

    final Element published = this.html.select("a.timestamp-link > abbr").first();
    return parseDateTime(published.attr("title"));
  }

  private LocalDateTime parseLastUpdated() {

    // Info unavailable in HTML data; default to same as published
    return this.published;
  }

  private List<String> parseCategories() {

    final Elements labels = this.html.select("span.post-labels > a");
    return
        labels
            .stream()
            .map(Element::text)
            .collect(Collectors.toList());
  }

  private @Nullable LocalDateTime parseDateTime(String published) {
    try {
      return
          LocalDateTime.parse(published);
    } catch (DateTimeParseException | NullPointerException e) {
      // Could not parse publish date
      return null;
    }
  }

  @Override
  public BloggerPost getBloggerPost() {
    return
        new BloggerPost(bloggerPostID, published, lastUpdated, categories, title, content, link);
  }
}