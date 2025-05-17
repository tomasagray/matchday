package net.tomasbot.matchday.plugin.datasource.forum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public final class RemoteDataReader {

  public static String readDataFrom(@NotNull URL url) throws IOException {
    try (final InputStreamReader in = new InputStreamReader(url.openStream());
        final BufferedReader reader = new BufferedReader(in)) {
      return reader.lines().collect(Collectors.joining("\n"));
    }
  }
}
