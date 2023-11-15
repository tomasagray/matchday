package self.me.matchday.util;

import java.io.*;
import org.apache.commons.net.telnet.TelnetClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class TelnetClientWrapper implements AutoCloseable {

  private static final int TIMEOUT = 5 * 1000;

  private final TelnetClient client;
  private PrintStream sender;
  private InputStream receiver;
  private String terminator;

  public TelnetClientWrapper() {
    this.client = new TelnetClient();
    client.setDefaultTimeout(TIMEOUT);
  }

  public String connect(@NotNull String host, int port, @NotNull String terminator)
      throws IOException {
    client.connect(host, port);
    this.sender = new PrintStream(client.getOutputStream());
    this.receiver = client.getInputStream();
    this.terminator = terminator;
    return receive();
  }

  public String send(@NotNull String data) throws IOException {
    sender.println(data);
    sender.flush();
    return receive();
  }

  public String receive() throws IOException {
    final StringBuilder sb = new StringBuilder();
    while (true) {
      final byte[] buffer = new byte[4 * 1024];
      final int read = receiver.read(buffer);
      final String data = new String(buffer, 0, read);
      if (!data.endsWith(terminator)) {
        sb.append(data);
      } else {
        sb.append(data, 0, data.indexOf(terminator));
        break;
      }
    }
    return sb.toString().trim();
  }

  public void disconnect() throws IOException {
    this.client.disconnect();
    this.sender.close();
    this.receiver.close();
    this.sender = null;
    this.receiver = null;
    this.terminator = null;
  }

  @Override
  public void close() throws Exception {
    disconnect();
  }
}
