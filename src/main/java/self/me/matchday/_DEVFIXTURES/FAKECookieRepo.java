package self.me.matchday._DEVFIXTURES;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.db.CookieRepository.FSPersistentCookie;
import self.me.matchday.util.Log;

// TODO: DELETE THIS!
public class FAKECookieRepo  {

  // =============================================================================================
  final String LOG_TAG = "CookieRepo";
  final String FILE_PATH =
      "C:\\Users\\Tomas\\Code\\Source\\IdeaProjects\\Matchday\\src\\test\\resources\\userdata.cookie";

  public List<HttpCookie> findAll() {
    final List<FSPersistentCookie> fsPersistentCookies = new ArrayList<>();

    try (FileInputStream fis = new FileInputStream(FILE_PATH);
        ObjectInputStream ois = new ObjectInputStream(fis)) {

      final FSPersistentCookie fsPersistentCookie = (FSPersistentCookie) ois.readObject();
      fsPersistentCookies.add(fsPersistentCookie);

    } catch (IOException | ClassNotFoundException e) {
      Log.d(LOG_TAG, "Could not read cookies from local storage", e);
    }

    return fsPersistentCookies.stream()
        .map(FSPersistentCookie::toHttpCookie)
        .collect(Collectors.toList());
  }
  public void save(@NotNull final HttpCookie cookie) {
    try(FileOutputStream fos = new FileOutputStream(FILE_PATH);
        ObjectOutputStream oos = new ObjectOutputStream(fos)) {

      oos.writeObject(FSPersistentCookie.fromHttpCookie(cookie));
    } catch (IOException e) {
      Log.d(LOG_TAG, "Could not write the cookie to local storage: " + cookie, e);
    }
  }
  // ===============================================================================================
}
