package self.me.matchday.plugin.fileserver.inclouddrive;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.io.TextFileReader;
import self.me.matchday.plugin.fileserver.FileServerUser;
import self.me.matchday.util.Log;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@DisplayName("Testing for InCloudDrive fileserver manager")
@TestMethodOrder(OrderAnnotation.class)
@Disabled
class ICDPluginTest {

    private static final String LOG_TAG = "ICDMTest";

    // Test constants
    private static final String LOGIN_DATA = "src/test/secure_resources/icd_test_login_data.csv";
    private static final String FILE_LINK =
            "https://www.inclouddrive.com/file/tnm6eehVDDGmykwxwnUx5Q/19980916-md1-manutd-barcelona-high-eng-1080p.mkv";
    private static URL FILE_URL;
    private static final String URL_PATTERN =
            "https://d\\d+\\.inclouddrive.com/download.php\\?accesstoken=[\\w-]+";

    // Test resources
    private final IcdPlugin icdPlugin;
    private static FileServerUser FileServerUser;

    ICDPluginTest(@Autowired final IcdPlugin icdPlugin) {
        this.icdPlugin = icdPlugin;
    }

    @BeforeAll
    static void setUp() throws IOException {

        // Read login data
        final String loginDataString = TextFileReader.readLocal(Path.of(LOGIN_DATA));
        Log.i(LOG_TAG, String.format("Read login data from %s: %s", LOGIN_DATA, loginDataString));
        // Split login data
        final String[] loginData = loginDataString.split(",");
        // Create fileserver user
        FileServerUser = new FileServerUser(loginData[0], loginData[1]);
        // Create file URL
        FILE_URL = new URL(FILE_LINK);
    }

    @Test
    @Order(1)
    @DisplayName("Testing login function")
    void login() {

        Log.i(LOG_TAG, String.format("Testing login functionality with user: %s", FileServerUser));
        // Attempt login of user
        final ClientResponse clientResponse = icdPlugin.login(FileServerUser);
        Log.i(LOG_TAG, "Login response: " + clientResponse);

        assertThat(clientResponse.statusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    @DisplayName("Properly determines that a supplied link is acceptable")
    void acceptsUrl() {

        Log.i(LOG_TAG, String.format("Testing link: %s", FILE_LINK));
        // Perform test
        assertThat(icdPlugin.acceptsUrl(FILE_URL)).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Can translate an input URL into a download URL")
    void getDownloadURL() throws IOException {

        Log.i(LOG_TAG, String.format("Getting download link for: %s", FILE_LINK));

        // Get download (direct) link
        final Optional<URL> downloadURL = icdPlugin.getDownloadURL(FILE_URL, new ArrayList<>());
        // Primary test
        assertThat(downloadURL.isPresent()).isTrue();

        downloadURL.ifPresent(url -> {
            Log.i(LOG_TAG, String.format("Successfully retrieved URL: %s", url));

            // Perform URL pattern test
            final Pattern pattern = Pattern.compile(URL_PATTERN);
            final boolean matches = pattern.matcher(url.toString()).find();
            assertThat(matches).isTrue();
        });
    }
}