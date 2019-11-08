package self.me.matchday.fileserver;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import self.me.matchday.io.JsonStreamReader;
import self.me.matchday.util.Log;

public class ICDManager implements IFSManager {
  private static final String LOG_TAG = "ICDManager";

  // Static members
  // -------------------------------------------------------------------------------------------
  private static final String DOWNLOAD_LINK_IDENTIFIER = "downloadnow";
  private static final String USER_DATA_IDENTIFIER = "doz";
  private static final String USER_AGENT
      // Windows
      // = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
      //      "(KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
      // Mac
      =
      "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) "
          + "Gecko/20100316 Firefox/3.6.2";

  // Singleton instance
  // -------------------------------------------------------------------------------------------
  private static volatile ICDManager INSTANCE;

  public static ICDManager getInstance() {
    if (INSTANCE == null) INSTANCE = new ICDManager();

    return INSTANCE;
  }

  // Fields
  // -----------------------------------------------------------------------------------------
  private final ICDCookieManager cookieManager;
  private FSUser user;
  private JsonObject loginResponse; // Response from server after latest login attempt
  private boolean isLoggedIn; // Current login status

  // Constructor
  // --------------------------------------------------------------------------------------------
  private ICDManager() {
    // Setup cookie management
    cookieManager = new ICDCookieManager();
    // Set default status to 'logged out'
    this.isLoggedIn = false;
  }

  // Public API
  // -----------------------------------------------------------------------------------------------------------------
  // TODO:
  //  Rewrite this function (and related functions) - division of tasks is unclear
  /** Perform user login and authentication to the file server, and save the returned cookies. */
  @Override
  public boolean login(@NotNull FSUser fsUser) {
    // Assume login will fail
    boolean loginSuccessful = false;
    // Save user instance
    this.user = fsUser;

    try {
      // Create POST connection & attach request data
      //   - Get login data
      byte[] loginData = user.getLoginDataByteArray();
      //   - Setup connection
      HttpURLConnection connection =
          setupICDPostConnection(ICDData.getLoginURL(), loginData.length);
      //   - Connect
      connection.connect();
      //   - POST login data to OutputStream
      try (OutputStream os = connection.getOutputStream()) {
        os.write(loginData);
      } catch (IOException e) {
        Log.e(LOG_TAG, "Could not write login data to output stream!", e);
      }

      // Read response as a JSON object
      loginResponse = JsonStreamReader.readJsonString(readServerResponse(connection));

      // If login attempt was successful
      loginSuccessful = isLoginSuccessful();
      if (loginSuccessful) {
        // Extract cookie from response, create userdata cookie
        cookieManager.addCookie("userdata", loginResponse.get(USER_DATA_IDENTIFIER).getAsString());

        Log.i(LOG_TAG, "Successfully logged in user: " + user.getUserName());
      } else {
        Log.i(LOG_TAG, "Failed to login with user: " + user.getUserName());
      }

    } catch (IOException e) {
      Log.e(LOG_TAG, "I/O Error while performing login function", e);
    }

    return loginSuccessful;
  }

  /** Clear saved cookies, requiring the user to re-authenticate. */
  @Override
  public void logout() {
    // Delete ALL cookies
    cookieManager.getCookieStore().removeAll();
    // Clear login response
    loginResponse = null;
    // Clear the user
    this.user = null;
  }

  @Override
  public boolean isLoggedIn() {
    // Update login boolean
    this.isLoggedIn = isLoginSuccessful();
    return this.isLoggedIn;
  }

  /**
   * Extract the direct download link from the ICD page.
   *
   * @param url The URL of the ICD page
   * @return An Optional containing the DD URL, if found
   */
  @Override
  public Optional<URL> getDownloadURL(@NotNull URL url)  {
    // By default, empty container
    Optional<URL> downloadLink = Optional.empty();

    try {
      // Open a connection
      URLConnection conn = url.openConnection();
      // Attach cookies
      conn.setRequestProperty("Cookie", cookieManager.getCookieString());
      // Connect to file server
      conn.connect();

      // Read the page from the file server & DOM-ify it
      Document filePage = Jsoup.parse(readServerResponse(conn));
      // Get all <a> with the 'downloadnow' class
      Elements elements = filePage.getElementsByClass(DOWNLOAD_LINK_IDENTIFIER);
      // - If we got a hit
      if (!elements.isEmpty()) {
        // - Extract href from <a>
        String theLink = elements.first().attr("href");
        downloadLink = Optional.of(new URL(theLink));
      }
    } catch (IOException e) {
      Log.e(LOG_TAG, "Could not parse download link from supplied URL: " + url, e);
    }

    return downloadLink;
  }

  /** Expose the login response to the API as a String */
  public String getLoginResponse() {
    return (loginResponse == null || loginResponse.isJsonNull()) ? null : loginResponse.toString();
  }

  // Server
  // -----------------------------------------------------------------------------------------------------------------
  /**
   * Prepares an HttpURLConnection for a given URL, with a given POST data size.
   *
   * @param url The URL we want a connection to
   * @param dataSize The size of the datagram that will be POSTed to this URL
   * @return HttpURLConnection A configured HTTP connection
   * @throws IOException If the connection cannot be opened
   */
  private HttpURLConnection setupICDPostConnection(@NotNull URL url, int dataSize)
      throws IOException {
    // Get an HTTP connection
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    // Set connection properties
    // - Make it a POST
    connection.setRequestMethod("POST");
    // - Enable I/O
    connection.setDoOutput(true);
    connection.setDoInput(true);
    // Set fixed data size
    connection.setFixedLengthStreamingMode(dataSize);
    connection.setRequestProperty(
        "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    // Set user agent (how we appear to server)
    connection.setRequestProperty("User-Agent", USER_AGENT);

    // Return the connection
    return connection;
  }

  /**
   * Read the response from the server on a given connection
   *
   * @param connection The connection to read from
   * @return A String containing the response from the server
   * @throws IOException If there is an error reading the data
   */
  @NotNull
  private String readServerResponse(URLConnection connection) throws IOException {
    StringBuilder response = new StringBuilder();
    try (InputStream is = connection.getInputStream()) {
      int i;
      // Read the response, one byte at a time
      // until the end of the stream
      while ((i = is.read()) != -1) {
        response.append((char) i);
      }
    }

    // Assemble and return response
    return response.toString();
  }

  /**
   * Determine if login has been successfully performed.
   *
   * @return A boolean indicating if the last login attempt was successful
   */
  private boolean isLoginSuccessful() {
    // Are we successfully logged into file server?
    boolean loggedIn = false;

    if (loginResponse != null) {
      // Determine result
      String result =
          loginResponse
              .get("result")
              .getAsString()
              .trim() // remove excess whitespace
              .toUpperCase(); // normalize

      if ("OK".equals(result)) loggedIn = true;
    }

    return loggedIn;
  }

  /** Helper class to hold data relevant to file-server (url, options, etc.) */
  static final class ICDData {
    // File server URL
    private static URL url;

    // User/access data
    // -------------------------------------------------------------------------------------
    private static final int me = 0;
    private static final String app = "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94";
    private static final String accessToken = "";

    // URL data
    // -------------------------------------------------------------------------------------
    private static final String protocol = "https://";
    private static final String domain = "inclouddrive.com";
    private static final String subDomain = "www";
    private static final String baseURL = protocol + subDomain + "." + domain + "/";
    private static final String loginUrl =
        baseURL + "api/" + me + "/signmein?useraccess=" + accessToken + "&access_token=" + app;

    static // Initialize file server URL
    {
      try {
        url = new URL(loginUrl);
      } catch (MalformedURLException e) {
        Log.e(LOG_TAG, "Error parsing InCloudDrive data!", e);
      }
    }

    @Contract(pure = true)
    static String getDomain() {
      return domain;
    }

    @NotNull
    @Contract(pure = true)
    static URL getLoginURL() {
      return url;
    }
  }
}
