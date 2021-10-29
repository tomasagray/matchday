/*
 * Copyright (c) 2021.
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

package self.me.matchday.plugin.datasource;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import self.me.matchday.TestDataCreator;
import self.me.matchday.model.*;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class TestDataSourcePlugin implements DataSourcePlugin<Event> {

  private final UUID pluginId = UUID.randomUUID();
  private final JpaRepository<DataSource, Long> dataSourceRepo;
  private final TestDataCreator testDataCreator;

  public TestDataSourcePlugin(TestDataSourceRepo dataSourceRepo, TestDataCreator testDataCreator) {
    this.dataSourceRepo = dataSourceRepo;
    this.testDataCreator = testDataCreator;
  }

  @Override
  public UUID getPluginId() {
    return this.pluginId;
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

  @Contract("_ -> new")
  @Override
  public @NotNull Snapshot<Event> getAllSnapshots(@NotNull SnapshotRequest request) {
    final Match testMatch = testDataCreator.createTestMatch();
    return Snapshot.of(Stream.of(testMatch));
  }

  @Contract(pure = true)
  @Override
  public @Nullable Snapshot<Event> getSnapshot(
      @NotNull SnapshotRequest request, @NotNull DataSource dataSource) {
    return Snapshot.of(Stream.of(testDataCreator.createTestMatch()));
  }

  @Contract(pure = true)
  @Override
  public @Nullable DataSource addDataSource(
      @NotNull URI baseUri, @NotNull List<VideoSourceMetadataPatternKit> metadataPatterns) {
    final TestDataSource entity = new TestDataSource(baseUri, metadataPatterns, this.getPluginId());
    System.out.println("Created test data source:\n" + entity);
    return dataSourceRepo.saveAndFlush(entity);
  }
}
