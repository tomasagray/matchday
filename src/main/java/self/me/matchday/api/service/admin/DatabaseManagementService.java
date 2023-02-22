package self.me.matchday.api.service.admin;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class DatabaseManagementService {

    private final MySqlDumpWrapper mySqlDumpWrapper;

    public DatabaseManagementService(MySqlDumpWrapper mySqlDumpWrapper) {
        this.mySqlDumpWrapper = mySqlDumpWrapper;
    }

    public Path createDatabaseDump() throws IOException {
        return mySqlDumpWrapper.dumpDatabase();
    }

    public void dropDatabase() {

    }

    public void installDatabase() {

    }
}
