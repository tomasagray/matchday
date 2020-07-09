package self.me.matchday.plugin.zkfootball;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import self.me.matchday.plugin.EventFileSourceParser;
import self.me.matchday.model.EventFile;
import self.me.matchday.model.EventFile.EventPartIdentifier;
import self.me.matchday.model.EventFileSource;

/**
 * Implementation of the Event File Source parser, specific to the ZKFootball blog, found at:
 * https://zkfootballmatch.blogspot.com
 */
public class ZKFEventFileSourceParser implements EventFileSourceParser {

  private final String html;
  private final List<EventFileSource> fileSources;

  public ZKFEventFileSourceParser(@NotNull final String html) {
    this.html = html;
    this.fileSources = parseEventFileSources();
  }

  /**
   * Parse out all EventFileSources from this post
   *
   * @return A List<> of EventFileSources (may be empty)
   */
  private @NotNull List<EventFileSource> parseEventFileSources() {

    // Prevent XSS attacks & parse HTML
    final String content = Jsoup.clean(html, Whitelist.basic());
    final Document dom = Jsoup.parse(content);

    // EventFileSources container
    final List<EventFileSource> fileSources = new ArrayList<>();
    // EventFiles container
    final Set<EventFile> eventFiles = new TreeSet<>();

    // Get all potential metadata identifiers
    final Elements elements = dom.select("b");
    elements.forEach(element -> {

      // EventFile
      if (EventPartIdentifier.isPartIdentifier(element.ownText())) {
        final Optional<EventFile> eventFile = getEventFile(element);
        eventFile.ifPresent(eventFiles::add);

        // EventFileSource
      } else if (ZKFPatterns.isMetadata(element.text())) {
        final EventFileSource fileSource =
            ZKFMetadataParser.createFileSource(element.select("span"));
        // Add EventFiles to the current EventFileSource
        fileSource.getEventFiles().addAll(eventFiles);
        // Add to collection
        fileSources.add(fileSource);
        // Reset EventFiles container
        eventFiles.clear();
      }
    });

    return fileSources;
  }

  /**
   * Given a DOM structure element, attempt to parse an EventFile
   *
   * @param element The DOM element which may contain an EventFile
   * @return An Optional<> which may contain an EventFile
   */
  private Optional<EventFile> getEventFile(@NotNull final Element element) {

    Optional<EventFile> result = Optional.empty();
    final String ownText = element.ownText();
    final EventPartIdentifier partIdentifier = EventPartIdentifier.fromString(ownText);

    // Find the next a[href] after the current element
    Element sibling = element.nextElementSibling();
    while (sibling != null) {
      if ("a".equals(sibling.tag().getName())) {
        // Extract link href
        final String href = sibling.attr("href");
        try {
          result = Optional.of(new EventFile(partIdentifier, new URL(href)));
        } catch (MalformedURLException ignore) {}
        break;  // only find one link
      }
      // advance token
      sibling = sibling.nextElementSibling();
    }
    return result;
  }

  @Override
  public List<EventFileSource> getEventFileSources() {
    return this.fileSources;
  }
}
