package net.tomasbot.matchday.plugin.datasource.forum;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import net.tomasbot.matchday.model.DataSource;
import net.tomasbot.matchday.model.Match;
import net.tomasbot.matchday.model.PatternKit;
import net.tomasbot.matchday.model.PlaintextDataSource;

@Component
public class ForumDataSourceValidator {

  void validateDataSourcePluginId(@NotNull UUID pluginId, @NotNull UUID dataSourcePluginId) {
    if (!pluginId.equals(dataSourcePluginId)) {
      throw new IllegalArgumentException(
          "DataSource is not a Forum datasource (Plugin ID does not match)");
    }
  }

  void validateDataSourcePatternKits(@NotNull PlaintextDataSource<?> dataSource) {
    List<PatternKit<? extends Match>> matchPatternKits = dataSource.getPatternKitsFor(Match.class);
    if (matchPatternKits == null || matchPatternKits.isEmpty()) {
      String msg =
          String.format("DataSource %s contains no Match patterns", dataSource.getDataSourceId());
      throw new IllegalArgumentException(msg);
    }
  }

  void validateDataSourceType(@NotNull DataSource<?> dataSource) {
    if (!(dataSource instanceof PlaintextDataSource)) {
      throw new IllegalArgumentException("DataSource is not a Hypertext data source");
    }
  }
}
