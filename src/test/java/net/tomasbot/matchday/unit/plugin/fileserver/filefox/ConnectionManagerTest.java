package net.tomasbot.matchday.unit.plugin.fileserver.filefox;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Validate FileFox plugin connection manager")
@Disabled
class ConnectionManagerTest {

  private static final Logger logger = LogManager.getLogger(ConnectionManagerTest.class);

  // TODO: implement this test!
  // NOTE: Requires upgrading project language level to >= 18
  // for SimpleFileServer interface, etc.

  private static void createTestServer() {
    InetSocketAddress address = new InetSocketAddress(65662);
    Path path = Path.of("/");
//        HttpServer server = SimpleFileServer.createFileServer(address, path,
//     SimpleFileServer.OutputLevel.VERBOSE);
    //    server.start();
  }

  @Test
  void connectTo() {}

  @Test
  void get() {}

  @Test
  void post() {}
}
