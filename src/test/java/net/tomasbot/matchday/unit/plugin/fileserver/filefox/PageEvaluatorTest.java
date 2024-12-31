package net.tomasbot.matchday.unit.plugin.fileserver.filefox;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import net.tomasbot.matchday.plugin.fileserver.filefox.FileFoxPage;
import net.tomasbot.matchday.plugin.fileserver.filefox.PageEvaluator;
import net.tomasbot.matchday.util.ResourceFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Validate FileFox plugin PageEvaluator")
class PageEvaluatorTest {

  private static final Logger logger = LogManager.getLogger(PageEvaluatorTest.class);

  private static final String TEST_DATA_ROOT = "data/filefox/";

  private final PageEvaluator pageEvaluator;

  @Autowired
  PageEvaluatorTest(PageEvaluator pageEvaluator) {
    this.pageEvaluator = pageEvaluator;
  }

  // === Page types ===
  // ✅ Invalid
  // ✅ DownloadLading
  // ✅ DirectDownload
  // ✅ Login
  // - Profile

  @Test
  @DisplayName("Validate parsing of error/invalid page")
  void testGetInvalidFileFoxPage() throws IOException {
    // given
    final String invalidPage = TEST_DATA_ROOT + "FileFox_Invalid.htm";

    // when
    FileFoxPage fileFoxPage = this.performPageAnalysis(invalidPage);

    // then
    assertThat(fileFoxPage).isInstanceOf(FileFoxPage.Invalid.class);
    if (fileFoxPage instanceof FileFoxPage.Invalid invalid) {
      assertThat(invalid.isLoggedIn()).isFalse();
      assertThat(invalid.isPremium()).isFalse();
      assertThat(invalid.getError()).isNotEmpty();
    }
  }

  @Test
  @DisplayName("Validate parsing of download landing page")
  void testGetDownloadLandingFoxPage() throws IOException {
    // given
    final String landingPage = TEST_DATA_ROOT + "FileFox_DownloadLanding.htm";
    final URI expectedDlUri = URI.create("/lfvdudvknyn4");
    final int expectedQueryParamCount = 6;

    // when
    FileFoxPage fileFoxPage = this.performPageAnalysis(landingPage);

    // then
    assertThat(fileFoxPage).isInstanceOf(FileFoxPage.DownloadLanding.class);
    if (fileFoxPage instanceof FileFoxPage.DownloadLanding landing) {
      assertThat(landing.isLoggedIn()).isTrue();
      assertThat(landing.isPremium()).isTrue();
      assertThat(landing.getDdlSubmitUri()).isNotNull().isEqualTo(expectedDlUri);
      assertThat(landing.getHiddenQueryParams().size()).isEqualTo(expectedQueryParamCount);
    }
  }

  @Test
  @DisplayName("Validate parsing of direct download page")
  void testGetDirectDownloadFoxPage() throws IOException {
    // given
    final String downloadPage = TEST_DATA_ROOT + "FileFox_DirectDownload.htm";
    final URL expectedDlUrl = new URL("https://s12.filefox.cc/SOMETHING/A_FILE.ext");

    // when
    FileFoxPage fileFoxPage = this.performPageAnalysis(downloadPage);

    // then
    assertThat(fileFoxPage).isInstanceOf(FileFoxPage.DirectDownload.class);
    if (fileFoxPage instanceof FileFoxPage.DirectDownload download) {
      assertThat(download.isLoggedIn()).isTrue();
      assertThat(download.isPremium()).isTrue();
      assertThat(download.getDdlUrl()).isEqualTo(expectedDlUrl);
    }
  }

  @Test
  @DisplayName("Validate parsing of login page")
  void testGetLoginFoxPage() throws IOException {
    // given
    final String loginPage = TEST_DATA_ROOT + "FileFox_Login.htm";

    // when
    FileFoxPage fileFoxPage = this.performPageAnalysis(loginPage);

    // then
    assertThat(fileFoxPage).isInstanceOf(FileFoxPage.Login.class);
    if (fileFoxPage instanceof FileFoxPage.Login login) {
      assertThat(login.isLoggedIn()).isFalse();
      assertThat(login.isPremium()).isFalse();
      assertThat(login.getText()).isNotEmpty();
    }
  }

  @Test
  @DisplayName("Validate parsing of profile page")
  void testGetProfileFoxPage() throws IOException {
    // given
    final String profilePage = TEST_DATA_ROOT + "FileFox_Profile.htm";
    float expectedTrafficAvailable = 0.76f;

    // when
    FileFoxPage fileFoxPage = this.performPageAnalysis(profilePage);

    // then
    assertThat(fileFoxPage).isInstanceOf(FileFoxPage.Profile.class);
    if (fileFoxPage instanceof FileFoxPage.Profile profile) {
      assertThat(profile.isLoggedIn()).isTrue();
      assertThat(profile.isPremium()).isTrue();
      assertThat(profile.getText()).isNotEmpty();
      assertThat(profile.getTrafficAvailable()).isEqualTo(expectedTrafficAvailable);
    }
  }

  private FileFoxPage performPageAnalysis(@NotNull String page) throws IOException {
    logger.info("Reading page data from: {}", page);
    String pageHtml = ResourceFileReader.readTextResource(page);
    FileFoxPage fileFoxPage = pageEvaluator.getFileFoxPage(pageHtml);
    logger.info("Parser returned FileFox page: {}", fileFoxPage);
    return fileFoxPage;
  }
}
