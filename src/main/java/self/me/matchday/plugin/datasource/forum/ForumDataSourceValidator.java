package self.me.matchday.plugin.datasource.forum;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Match;
import self.me.matchday.model.PatternKit;
import self.me.matchday.model.PlaintextDataSource;

import java.util.List;
import java.util.UUID;

@Component
public class ForumDataSourceValidator {

    void validateDataSourcePluginId(@NotNull UUID pluginId, @NotNull UUID dataSourcePluginId) {
        if (!pluginId.equals(dataSourcePluginId)) {
            throw new IllegalArgumentException("DataSource is not a Forum datasource (Plugin ID does not match)");
        }
    }

    void validateDataSourcePatternKits(@NotNull PlaintextDataSource<?> dataSource) {
        List<PatternKit<? extends Match>> matchPatternKits = dataSource.getPatternKitsFor(Match.class);
        if (matchPatternKits == null || matchPatternKits.isEmpty()) {
            String msg = String.format("DataSource %s contains no Match patterns", dataSource.getDataSourceId());
            throw new IllegalArgumentException(msg);
        }
    }

    void validateDataSourceType(@NotNull DataSource<?> dataSource) {
        if (!(dataSource instanceof PlaintextDataSource)) {
            throw new IllegalArgumentException("DataSource is not a Hypertext data source");
        }
    }
}