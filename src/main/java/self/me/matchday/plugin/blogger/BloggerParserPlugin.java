package self.me.matchday.plugin.blogger;

import java.util.stream.Stream;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.plugin.DataSourcePlugin;
import self.me.matchday.plugin.blogger.parser.PostParserFactory;

@Data
public abstract class BloggerParserPlugin implements DataSourcePlugin<Stream<Event>> {

  protected final BloggerPlugin bloggerPlugin;
  protected final PostParserFactory postParserFactory;

  protected Stream<Event> getEventStream(@NotNull final Blogger blogger) {
    return
        blogger
          .getPosts()
          .map(bloggerPost ->
              // Post parser factory supplied by subclasses
              getPostParserFactory()
                  .createParser(bloggerPost)
                  .getEvent());
  }
}
