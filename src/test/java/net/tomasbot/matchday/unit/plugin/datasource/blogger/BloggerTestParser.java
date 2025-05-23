package net.tomasbot.matchday.unit.plugin.datasource.blogger;

import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.PatternKit;
import net.tomasbot.matchday.model.PlaintextDataSource;
import net.tomasbot.matchday.plugin.datasource.parsing.DataSourceParser;
import net.tomasbot.matchday.plugin.datasource.parsing.TextParser;

@Component
public final class BloggerTestParser implements DataSourceParser<BloggerTestEntity, String> {

  private final TextParser textParser;

  public BloggerTestParser(TextParser textParser) {
    this.textParser = textParser;
  }

  @SuppressWarnings("unchecked cast")
  @Override
  public Stream<? extends BloggerTestEntity> getEntityStream(
      @NotNull DataSource<? extends BloggerTestEntity> dataSource, @NotNull String data) {
    // get PatternKits
    final PlaintextDataSource<BloggerTestEntity> plaintextDataSource =
        (PlaintextDataSource<BloggerTestEntity>) dataSource;
    List<PatternKit<? extends BloggerTestEntity>> patternKits =
        plaintextDataSource.getPatternKitsFor(BloggerTestEntity.class);

    // get text
    Document document = Jsoup.parse(data);
    String text = document.text();

    return textParser.createEntityStreams(patternKits, text);
  }

  @Override
  public Class<BloggerTestEntity> getType() {
    return BloggerTestEntity.class;
  }
}
