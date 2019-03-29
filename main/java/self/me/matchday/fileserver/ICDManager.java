package self.me.matchday.fileserver;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import self.me.matchday.io.JsonStreamReader;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ICDManager implements IFSManager
{
    private static final String LOG_TAG = "ICDManager";


    // Static members
    private static final String USER_AGENT
            // Windows
            // = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            //      "(KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36";
            // Mac
            = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) " +
                "Gecko/20100316 Firefox/3.6.2";
    private static final String DOWNLOAD_LINK_IDENTIFIER = "downloadnow";
    // -------------------------------------------------------------------------------------------

    // Singleton instance
    private static final ICDManager INSTANCE = new ICDManager();
    public static ICDManager getInstance() {
        return INSTANCE;
    }
    // ----------------------------------------------------------------------------


    // Fields
    // -----------------------------------------------------------------------------------------
    private FSUser user;
    private JsonObject loginResponse;   // Response from server after latest login attempt
    private boolean isLoggedIn;         // Current login status
    private final CookieManager cookieManager;

    // Constructor
    // --------------------------------------------------------------------------------------------
    private ICDManager()
    {
        // Setup cookie management
        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy( new ICDCookiePolicy(ICDData.getDomain()) );
        CookieHandler.setDefault(cookieManager);

        // Set default status to 'logged out'
        this.isLoggedIn = false;
    }

    // Public API
    // -----------------------------------------------------------------------------------------------------------------
    /**
     * Perform user login and authentication to the file server, and save the
     * returned cookies.
     *
     */
    @Override
    public boolean login(FSUser fsUser)
    {
        // Assume login will fail
        boolean loginSuccessful = false;
        // Save user instance
        this.user = fsUser;

        try {
            // Create POST connection & attach request data
            //   - Get login data
            byte[] loginData = user.getLoginDataByteArray();
            //   - Setup connection
            HttpURLConnection connection
                    = setupICDPostConnection( ICDData.getLoginURL(), loginData.length );
            //   - Connect
            connection.connect();
            //   - POST login data to OutputStream
            try( OutputStream os = connection.getOutputStream() ) {
                os.write( loginData );
            }

            // Read response as a JSON object
            loginResponse =
                    JsonStreamReader.readJsonString (
                        readServerResponse(connection)
                    );

            // If login attempt was successful
            loginSuccessful = isLoginSuccessful();
            if( loginSuccessful )
            {
                // Extract cookie from response
                // - Get userdata cookie data
                String userdata =
                        URLEncoder.encode(
                            loginResponse.get("doz").getAsString(),
                            StandardCharsets.UTF_8.toString()
                        );

                // - Create userdata cookie
                HttpCookie userDataCookie = new HttpCookie("userdata", userdata );
                userDataCookie.setDomain( "." + ICDData.getDomain() );
                // - Add to cookie store
                cookieManager
                    .getCookieStore()
                    .add(
                        new URI( userDataCookie.getDomain() ),
                        userDataCookie
                    );

                Log.i(LOG_TAG, "Successfully logged in user: " + user.getUserName());
            } else {
                Log.i(LOG_TAG, "Failed to login with user: " + user.getUserName());
            }

        } catch(IOException | URISyntaxException e) {
            // TODO:
            //  - Handle login exceptions
            Log.e(LOG_TAG, "Error while performing login function; please check URL", e);
        }

        return loginSuccessful;
    }

    @Override
    public void logout()
    {
        // TODO:
        //  - Implement logout functionality

        // Clear the user
        this.user = null;
    }

    @Override
    public boolean isLoggedIn()
    {
        // Update login boolean
        this.isLoggedIn = isLoginSuccessful();
        return this.isLoggedIn;
    }

    /**
     * Extract the direct download link from the ICD page.
     *
     * @param url The URL of the ICD page
     * @return An Optional containing the DD URL, if found
     *
     * @throws IOException If any problems connecting to, or reading the page
     */
    @Override
    public Optional<URL> getDownloadURL(URL url) throws IOException
    {
        Optional<URL> downloadLink = Optional.empty();

        // Open a connection
        URLConnection conn = url.openConnection();
        // Attach cookies
        conn.setRequestProperty("Cookie", getCookieString() );
        // Connect to file server
        conn.connect();
        // Read the page from the file server & DOM-ify it
        Document filePage = Jsoup.parse( readServerResponse(conn) );
        // Get all <a> with the 'downloadnow' class
        Elements elements = filePage.getElementsByClass(DOWNLOAD_LINK_IDENTIFIER);
        // - If we got a hit
        if( !elements.isEmpty() )
        {
            // - Extract href from <a>
            String theLink = elements.first().attr("href");
            downloadLink = Optional.of( new URL(theLink) );
        }

        return downloadLink;
    }
    // -----------------------------------------------------------------------------------------------------------------


    // Cookies
    // -----------------------------------------------------------------------------------------------------------------
    private boolean saveCookieData()
    {
        // TODO:
        //  - Implement persistent cookie storage

        return false;
    }


    private List<HttpCookie> loadCookieData()
    {
        List<HttpCookie> cookies = new ArrayList<>();
        // TODO:
        //  - Implement persistent cookie loading

        return cookies;
    }

    @NotNull
    private String getCookieString()
    {
        // Container for cookie String
        StringBuilder sb = new StringBuilder();
        // Our cookies
        List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
        // Add each cookie to String
        cookies.forEach((cookie) ->
                sb
                        .append( cookie.getName() )
                        .append("=")
                        .append( cookie.getValue() )
                        .append("; ")   // separator
        );

        // Return assembled String
        return sb.toString();
    }
    // -----------------------------------------------------------------------------------------------------------------

    // Server
    // -----------------------------------------------------------------------------------------------------------------
    /**
     * Prepares an HttpURLConnection for a given URL, with a given POST data size.
     *
     * @param  url                 The URL we want a connection to
     * @param  dataSize                The size of the datagram that will be POSTed to this URL
     * @return HttpURLConnection   A configured HTTP connection
     * @throws IOException          If the connection cannot be opened
     *
     */
    private HttpURLConnection setupICDPostConnection(@NotNull URL url, int dataSize) throws IOException
    {
        // Get an HTTP connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set connection properties
        // - Make it a POST
        connection.setRequestMethod("POST");
        // - Enable I/O
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // Set fixed data size
        connection.setFixedLengthStreamingMode( dataSize );
        connection.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
        );
        // Set user agent (how we appear to server)
        connection.setRequestProperty(
                "User-Agent",
                ICDManager.USER_AGENT
        );

        // Return the connection
        return connection;
    }

    /**
     * Read the response from the server on a given connection
     * @param connection The connection to read from
     * @return A String containing the response from the server
     * @throws IOException If there is an error reading the data
     */
    @NotNull
    private String readServerResponse(URLConnection connection) throws IOException
    {
        StringBuilder response = new StringBuilder();
        try(InputStream is = connection.getInputStream()) {
            int i;
            // Read the response, one byte at a time
            // until the end of the stream
            while ((i = is.read()) != -1)
                response.append((char) i);
        }

        // Assemble and return response
        return response.toString();
    }

    /**
     * Determine if login has been successfully performed.
     *
     * @return A boolean indicating if the last login attempt was successful
     */
    private boolean isLoginSuccessful()
    {
        // Are we successfully logged into file server?
        boolean loggedIn = false;

        if( loginResponse != null )
        {
            // Determine result
            String result
                    = loginResponse
                        .get("result")
                        .getAsString()
                        .trim()             // remove excess whitespace
                        .toUpperCase();     // normalize

            if( "OK".equals( result ) )
                loggedIn = true;
        }

        return loggedIn;
    }
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * Helper class to hold data relevant to file-server
     * (url, options, etc.)
     *
     */
    static final class ICDData
    {
        // File server URL
        private static URL url;

        // URL data
        // -------------------------------------------------------------------------------------
        private static final String protocol = "https://";
        private static final String domain = "inclouddrive.com";
        private static final String subDomain = "www";
        private static final String baseURL
                = protocol + subDomain + "." + domain + "/";

        // User/access data
        // -------------------------------------------------------------------------------------
        private static final int    me      = 0;
        private static final String app     = "br68ufmo5ej45ue1q10w68781069v666l2oh1j2ijt94";
        private static final String accessToken = "";

        // Initialize file server URL
        static
        {
            try {
                url = new URL(
                        baseURL
                                + "api/"
                                + me
                                + "/signmein?useraccess="
                                + accessToken
                                + "&access_token="
                                + app
                );
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
