package self.me.matchday.api.service.admin;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseManagementService {

  private static final Pattern versionPattern = Pattern.compile("^([\\d.]+)");

  private final MySqlDumpWrapper mySqlDumpWrapper;
  private final DataSource dataSource;
  private Integer mysqlVersion;

  public DatabaseManagementService(MySqlDumpWrapper mySqlDumpWrapper, DataSource dataSource) {
    this.mySqlDumpWrapper = mySqlDumpWrapper;
    this.dataSource = dataSource;
  }

  public Path createDatabaseDump() throws IOException {
    return mySqlDumpWrapper.dumpDatabase();
  }

  @Transactional
  public void installDatabase(@NotNull Path dumpFile) throws SQLException, IOException {

    int version = getMysqlVersion();
    final Resource resource = new FileSystemResource(dumpFile);
    final ResourceDatabasePopulator populator =
        new ConditionalStatementResourcePopulator(version, resource);
    populator.execute(dataSource);
  }

  public int getMysqlVersion() throws SQLException {
    if (mysqlVersion == null) {
      mysqlVersion = fetchMysqlVersion();
    }
    return mysqlVersion;
  }

  private int fetchMysqlVersion() throws SQLException {
    final Connection connection = dataSource.getConnection();
    final PreparedStatement statement = connection.prepareStatement("SELECT VERSION()");
    final ResultSet resultSet = statement.executeQuery();
    if (resultSet.next()) {
      final String version = resultSet.getString(1);
      final Matcher matcher = versionPattern.matcher(version);
      if (matcher.find()) {
        final String versionData =
            Arrays.stream(matcher.group(1).split("\\."))
                .map(subversion -> subversion.length() < 2 ? subversion + "0" : subversion)
                .collect(Collectors.joining());
        return Integer.parseInt(versionData);
      }
    }
    throw new SQLException("Could not determine MySQL version");
  }
}
