package self.me.matchday._DEVFIXTURES;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpCookie;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import self.me.matchday.util.Log;

@TestMethodOrder(OrderAnnotation.class)
class FAKECookieRepoTest {

  private static final String LOG_TAG = "FAKECookieRepoTest";

  private static final String NAME = "FAKE_COOKIE_NAME";
  private static final String VALUE = "FAKE VALUE";

  private static FAKECookieRepo fakeCookieRepo;

  @BeforeAll
  static void setup() {
    // Create instance
    fakeCookieRepo = new FAKECookieRepo();
  }

  @Test
  @Order(1)
  void save() {

    // Create cookie
    final HttpCookie httpCookie = new HttpCookie(NAME, VALUE);
    // Save cookie to storage
    fakeCookieRepo.save(httpCookie);
  }

  @Test
  @Order(2)
  void findAll() {

    // Find saved cookie
    List<HttpCookie> cookies = fakeCookieRepo.findAll();
    // Pre-test
    assertTrue(cookies.size() > 0);
    Log.i(LOG_TAG, String.format("Found: %s cookies", cookies.size()));
    cookies.forEach(cookie -> {
      // Perform test
      assert cookie.getValue().equals(VALUE);
      Log.i(LOG_TAG, String.format("Cookie value is: %s", VALUE));
    });
  }
}