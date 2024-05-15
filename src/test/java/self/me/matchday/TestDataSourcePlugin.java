/*
 * Copyright (c) 2022.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;

import java.net.URL;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class TestDataSourcePlugin implements DataSourcePlugin {

    public static final UUID PLUGIN_ID = UUID.fromString("37149b7c-4dae-48c2-997a-a7427628b408");
    private final TestDataCreator testDataCreator;

    public TestDataSourcePlugin(TestDataCreator testDataCreator) {
        this.testDataCreator = testDataCreator;
    }

    @Override
    public UUID getPluginId() {
        return PLUGIN_ID;
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getTitle() {
        return "Test data source plugin";
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDescription() {
        return "A description";
    }

    @Override
    @Contract(pure = true)
    @SuppressWarnings("unchecked cast")
    public <T> Snapshot<T> getSnapshot(
            @NotNull SnapshotRequest request, @NotNull DataSource<T> dataSource) {
        return Snapshot.of(Stream.of(testDataCreator.createTestMatch()).map(obj -> (T) obj));
    }

    @Override
    public <T> Snapshot<T> getUrlSnapshot(@NotNull URL url, @NotNull DataSource<T> dataSource) {
        return getSnapshot(SnapshotRequest.builder().build(), dataSource);
    }

    @Override
    public void validateDataSource(@NotNull DataSource<?> dataSource) {
        if (!dataSource.getPluginId().equals(getPluginId())) {
            throw new IllegalArgumentException("Wrong pluginId for TestDataSourcePlugin");
        }
    }
}
