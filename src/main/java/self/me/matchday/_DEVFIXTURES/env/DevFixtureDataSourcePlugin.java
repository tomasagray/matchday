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

package self.me.matchday._DEVFIXTURES.env;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.plugin.datasource.DataSourcePlugin;

// @Component
public class DevFixtureDataSourcePlugin implements DataSourcePlugin {

  private static final Logger logger = LogManager.getLogger(DevFixtureDataSourcePlugin.class);
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  // TODO: Delete this class & related when no longer necessary
  private final UUID pluginId = UUID.fromString("b7098ba3-862e-4cee-a62d-a8cdd0eeef3b");
  private boolean enabled = true;

  @Override
  public UUID getPluginId() {
    return this.pluginId;
  }

  @Override
  public String getTitle() {
    return "Development DataSourcePlugin";
  }

  @Override
  public String getDescription() {
    return "Convenience fixture for development";
  }

  @Override
  public void validateDataSource(@NotNull DataSource<?> dataSource) {
    final UUID pluginId = dataSource.getPluginId();
    if (!this.pluginId.equals(pluginId)) {
      throw new IllegalArgumentException("Incorrect plugin ID: " + pluginId);
    }
    final URI baseUri = dataSource.getBaseUri();
    if (!baseUri.toString().startsWith("http://hal9000:7000")) {
      throw new IllegalArgumentException("Incorrect url: " + baseUri);
    }
  }

  @Override
  @SuppressWarnings("unchecked cast")
  public <T> Snapshot<T> getSnapshot(
      @NotNull SnapshotRequest request, @NotNull DataSource<T> dataSource) throws IOException {

    final URL baseUrl = dataSource.getBaseUri().toURL();
    try (final BufferedReader reader =
        new BufferedReader(new InputStreamReader(baseUrl.openConnection().getInputStream()))) {

      logger.info(
          "Getting Snapshot of DataSource: {} @ URL: {}", dataSource.getDataSourceId(), baseUrl);
      final String data = reader.lines().collect(Collectors.joining("\n"));
      System.out.println("Using data: " + data);
      final Type type = new TypeReference<List<Event>>() {}.getType();
      final List<Event> events = gson.fromJson(data, type);
      logger.info("Got Events:\n{}", events);
      return (Snapshot<T>) Snapshot.of(events.stream());
    }
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
