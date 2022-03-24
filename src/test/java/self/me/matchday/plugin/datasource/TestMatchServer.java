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

package self.me.matchday.plugin.datasource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.Event;
import self.me.matchday.model.Snapshot;
import self.me.matchday.model.SnapshotRequest;
import self.me.matchday.model.video.PartIdentifier;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TestMatchServer implements DataSourcePlugin {

  public static final String TEST_SERVER_URL = "http://192.168.0.107:7000/?format=json";
  private static final UUID PLUGIN_ID = UUID.fromString("3acb0e50-9733-44c7-96c2-98908cc508f1");
  private static final Gson gson;
  private static URL url;

  static {
    try {
      url = new URL(TEST_SERVER_URL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      System.exit(1);
    }
    gson = getGson();
  }

  @NotNull
  private static Gson getGson() {
    return new GsonBuilder()
        //        .registerTypeAdapter(
        //            LocalDateTime.class,
        //            (JsonDeserializer<LocalDateTime>)
        //                (json, type, jsonDeserializationContext) -> {
        //                  final String dateStr = json.getAsJsonPrimitive().getAsString();
        //                  return LocalDateTime.parse(dateStr);
        //                })
        //        .registerTypeAdapter(
        //            VideoFile.class,
        //            (JsonDeserializer<VideoFile>)
        //                (json, type, context) -> {
        //                  try {
        //                    final String str = json.getAsString();
        //                    final URL externalUrl = new URL(str);
        //                    final PartIdentifier partIdentifier =
        //                        TestMatchServer.getPartIdentifier(externalUrl);
        //                    return new VideoFile(partIdentifier, externalUrl);
        //                  } catch (MalformedURLException e) {
        //                    e.printStackTrace();
        //                    return null;
        //                  }
        //                })
        .create();
  }

  private static PartIdentifier getPartIdentifier(@NotNull final URL url) {

    final Pattern pattern = Pattern.compile("_(\\w)[\\-]?[\\w]*.(mkv|ts)$");
    final Matcher matcher = pattern.matcher(url.toString());
    if (matcher.find()) {
      final String identifier = matcher.group(1);
      if ("E".equals(identifier)) {
        return PartIdentifier.EXTRA_TIME;
      }

      final int partNum = Integer.parseInt(identifier);
      switch (partNum) {
        case 0:
          return PartIdentifier.PRE_MATCH;
        case 1:
          return PartIdentifier.FIRST_HALF;
        case 2:
          return PartIdentifier.SECOND_HALF;
        case 3:
          return PartIdentifier.POST_MATCH;
        default:
          return PartIdentifier.DEFAULT;
      }
    }
    return PartIdentifier.DEFAULT;
  }

  @Override
  public UUID getPluginId() {
    return PLUGIN_ID;
  }

  @Override
  public String getTitle() {
    return "Test Match Server (localhost)";
  }

  @Override
  public String getDescription() {
    return "Simple match data server for testing";
  }

  public Snapshot<Event> getAllSnapshots() throws IOException {
    Event[] matches = gson.fromJson(TextFileReader.readRemote(url), Event[].class);
    return new Snapshot<>(Arrays.stream(matches));
  }

  @Override
  public <T> Snapshot<T> getSnapshot(
      @NotNull SnapshotRequest request, @NotNull DataSource<T> dataSource) {
    return null;
  }

  @Override
  public void validateDataSource(@NotNull DataSource<?> dataSource) {
    if (!dataSource.getPluginId().equals(this.getPluginId())) {
      throw new IllegalArgumentException("Wrong DataSource pluginId!");
    }
  }
}
