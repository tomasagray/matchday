package self.me.matchday.api.service.admin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.SettingsService;

@Component
public class MySqlDumpWrapper {

  private static final Pattern urlPattern = Pattern.compile("[\\w:]+//([\\w.]+):(\\d+)/(\\w+)?");
  private static final String filenamePrefix = "matchday_dump_";
  private final SettingsService settingsService;

  @Value("${application.datasource.defaults-file}")
  private String defaultsFile;

  @Value("${spring.datasource.url}")
  private String dataSourceUrl;

  public MySqlDumpWrapper(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @Contract("_ -> new")
  private static @NotNull MysqlDump executeDump(@NotNull String cmd) throws IOException {

    final Process dumpProcess = Runtime.getRuntime().exec(cmd);
    final LinkedList<String> data = new LinkedList<>();
    final LinkedList<String> error = new LinkedList<>();
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

  private String getDumpCommand() {
    final Matcher matcher = urlPattern.matcher(this.dataSourceUrl);
    if (matcher.find()) {
      final String host = matcher.group(1);
      final int port = Integer.parseInt(matcher.group(2));
      final String database = matcher.group(3);
      return String.format(
          "mysqldump --defaults-file=%s -h %s -P %d -uroot --add-drop-database --databases %s",
          this.defaultsFile, host, port, database);
    }
    throw new IllegalArgumentException("Could not parse database URL: " + this.dataSourceUrl);
  }

  @NotNull
  private Path createBackupFilepath() throws IOException {
    // create file path
    final Path backupLocation = getBackupLocation();
    final long timestamp = Instant.now().toEpochMilli();
    final String filename = String.format("%s%s.sql", filenamePrefix, timestamp);
    return backupLocation.resolve(filename);
  }

  private @NotNull Path getBackupLocation() throws IOException {
    final Path backupLocation = settingsService.getSettings().getBackupLocation();
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
