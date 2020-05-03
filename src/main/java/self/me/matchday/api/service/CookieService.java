package self.me.matchday.api.service;

import java.net.HttpCookie;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday._DEVFIXTURES.FAKECookieRepo;
import self.me.matchday.db.CookieRepository;
import self.me.matchday.db.CookieRepository.FSPersistentCookie;

@Service
public class CookieService {

  private static final String LOG_TAG = "CookieService";

  private final CookieRepository cookieRepository;
  private final FAKECookieRepo fakeCookieRepo;

  @Autowired
  public CookieService(CookieRepository cookieRepository) {

    this.cookieRepository = cookieRepository;
    // =============================================================================================
    // TODO: FOR DEVELOPMENT ONLY!
    this.fakeCookieRepo = new FAKECookieRepo();
    // =============================================================================================
  }

  /**
   * Return all saved cookies from the repo.
   * @return All HttpCookies saved in local storage.
   */
  public List<HttpCookie> fetchAll() {

    // =============================================================================================
    // TODO: FOR DEVELOPMENT ONLY!
    return fakeCookieRepo.findAll();
    // =============================================================================================
   /* return
        cookieRepository
            .findAll()
            .stream()
            .map(FSPersistentCookie::toHttpCookie)
            .collect(Collectors.toList());*/
  }

  /**
   * Save a cookie to local storage.
   * @param cookie An HttpCookie to save.
   */
  public void saveCookie(@NotNull final HttpCookie cookie) {

    // =============================================================================================
    // TODO: FOR DEVELOPMENT ONLY!
    fakeCookieRepo.save(cookie);
    // =============================================================================================
    cookieRepository.save(FSPersistentCookie.fromHttpCookie(cookie));
  }
}
