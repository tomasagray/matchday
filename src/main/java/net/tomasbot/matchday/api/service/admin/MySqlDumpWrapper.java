package net.tomasbot.matchday.api.service.admin;

import static net.tomasbot.matchday.config.settings.BackupLocation.BACKUP_LOCATION;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.tomasbot.matchday.api.service.SettingsService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MySqlDumpWrapper {

  private static final Pattern urlPattern = Pattern.compile("[\\w:]+//([\\w.]+):(\\d+)/(\\w+)?");
  private static final String filenamePrefix = "matchday_dump_";
  private final SettingsService settingsService;

  @Value("${spring.datasource.url}")
  private String dataSourceUrl;

  public MySqlDumpWrapper(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  public String getVersion() throws IOException {
    List<String> commands = List.of("mysqldump", "--version");
    Process process = new ProcessBuilder().command(commands).start();

    try (InputStreamReader in = new InputStreamReader(process.getInputStream());
        BufferedReader reader = new BufferedReader(in)) {
      return reader.readLine();
    } finally {
      process.destroy();
    }
  }

  @Contract("_ -> new")
  private static @NotNull MysqlDump executeDump(@NotNull String cmd) throws IOException {
    final LinkedList<String> data = new LinkedList<>();
    final LinkedList<String> error = new LinkedList<>();
    final Process dumpProcess = Runtime.getRuntime().exec(cmd);
    try (final InputStream is = dumpProcess.getInputStream();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final InputStream es = dumpProcess.getErrorStream();
        final BufferedReader errReader = new BufferedReader(new InputStreamReader(es))) {

      String output, errOutput = null;
      while ((output = reader.readLine()) != null || (errOutput = errReader.readLine()) != null) {
        if (output != null) {
          data.add(output);
        }
        if (errOutput != null) {
          error.add(errOutput);
        }
      }
    } finally {
      // Ensure process closed
      dumpProcess.destroy();
    }
    return new MysqlDump(data.toArray(new String[0]), error.toArray(new String[0]));
  }

  private static void writeDumpToDisk(@NotNull Path filepath, @NotNull MysqlDump mysqlDump)
      throws IOException {
    try (FileOutputStream fos = new FileOutputStream(filepath.toFile())) {
      final String[] data = mysqlDump.data();
      for (final String datum : data) {
        final String line = datum + "\n";
        fos.write(line.getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  public Path dumpDatabase() throws IOException {
    final Path filepath = createBackupFilepath();
    final String cmd = getDumpCommand();
    final MysqlDump mysqlDump = executeDump(cmd);
    final String[] error = mysqlDump.error();
    if (error.length > 0) {
      throw new IOException("Error creating database dump: " + Arrays.toString(error));
    }
    writeDumpToDisk(filepath, mysqlDump);
    return filepath;
  }

  @NotNull
  private String getDumpCommand() {
    final Matcher matcher = urlPattern.matcher(this.dataSourceUrl);
    if (matcher.find()) {
      final String host = matcher.group(1);
      final int port = Integer.parseInt(matcher.group(2));
      final String database = matcher.group(3);
      final String additionalOpts = "--add-drop-database --no-tablespaces";
      return String.format(
          "mysqldump -h %s -P %d %s --databases %s", host, port, additionalOpts, database);
    }
    throw new IllegalArgumentException("Could not parse database URL: " + this.dataSourceUrl);
  }

  @NotNull
  private Path createBackupFilepath() throws IOException {
    final Path backupLocation = getBackupLocation();
    final long timestamp = Instant.now().toEpochMilli();
    final String filename = String.format("%s%s.sql", filenamePrefix, timestamp);
    return backupLocation.resolve(filename);
  }

  private @NotNull Path getBackupLocation() throws IOException {
    final Path backupLocation = settingsService.getSetting(BACKUP_LOCATION, Path.class);
    final File backupDir = backupLocation.toFile();
    if (!backupDir.exists()) {
      final boolean created = backupDir.mkdirs();
      if (!created) {
        throw new IOException("Could not create backup directory: " + backupDir);
      }
    }
    return backupLocation;
  }

  record MysqlDump(String[] data, String[] error) {}
}
