package self.me.matchday.api.service;

import java.net.HttpCookie;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import self.me.matchday.db.CookieRepository;
import self.me.matchday.db.CookieRepository.FSPersistentCookie;

@Service
public class CookieService {

  @Autowired
  private CookieRepository cookieRepository;

  /**
   * Return all saved cookies from the repo.
   * @return All HttpCookies saved in local storage.
   */
  public List<HttpCookie> fetchAll() {
    return
        cookieRepository
            .findAll()
            .stream()
            .map(FSPersistentCookie::toHttpCookie)
            .collect(Collectors.toList());
  }

  /**
   * Save a cookie to local storage.
   * @param cookie An HttpCookie to save.
   */
  public void saveCookie(@NotNull final HttpCookie cookie) {
    cookieRepository.save(FSPersistentCookie.fromHttpCookie(cookie));
  }
}
