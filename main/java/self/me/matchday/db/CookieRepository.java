package self.me.matchday.db;

import java.io.Serializable;
import java.net.HttpCookie;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import self.me.matchday.db.CookieRepository.FSPersistentCookie;

@Repository
public interface CookieRepository extends JpaRepository<FSPersistentCookie, Long> {

  // TODO: Delete this repo?

  default List<HttpCookie> findAllHttpCookies() {
    return this.findAll().stream()
        .map(FSPersistentCookie::toHttpCookie)
        .collect(Collectors.toList());
  }

  /** Wrapper class to persist java.net.HttpCookie */
  @Entity
  @Data
  @NoArgsConstructor
  class FSPersistentCookie implements Serializable {

    @Id @GeneratedValue private Long cookieId;
    private String domain;
    private String name;
    private String value;
    private int version;
    private String comment;
    private String commentURL;
    private boolean discard;
    private long maxAge;
    private String path;
    private String portList;
    private boolean secure;

    private FSPersistentCookie(
        String name,
        String value,
        String domain,
        int version,
        String comment,
        String commentURL,
        boolean discard,
        long maxAge,
        String path,
        String portList,
        boolean secure) {
      this.name = name;
      this.value = value;
      this.domain = domain;
      this.version = version;
      this.comment = comment;
      this.commentURL = commentURL;
      this.discard = discard;
      this.maxAge = maxAge;
      this.path = path;
      this.portList = portList;
      this.secure = secure;
    }

    @NotNull
    @Contract("_ -> new")
    public static FSPersistentCookie fromHttpCookie(@NotNull final HttpCookie cookie) {
      String name = cookie.getName();
      String value = cookie.getValue();
      String domain = cookie.getDomain();
      int version = cookie.getVersion();
      String comment = cookie.getComment();
      String commentURL = cookie.getCommentURL();
      boolean discard = cookie.getDiscard();
      long maxAge = cookie.getMaxAge();
      String path = cookie.getPath();
      String portlist = cookie.getPortlist();
      boolean secure = cookie.getSecure();

      return new FSPersistentCookie(
          name,
          value,
          domain,
          version,
          comment,
          commentURL,
          discard,
          maxAge,
          path,
          portlist,
          secure);
    }

    @NotNull
    public static HttpCookie toHttpCookie(@NotNull final FSPersistentCookie persistentCookie) {

      // Copy values
      final HttpCookie httpCookie =
          new HttpCookie(persistentCookie.getName(), persistentCookie.getValue());
      httpCookie.setDomain(persistentCookie.getDomain());
      httpCookie.setVersion(persistentCookie.getVersion());
      httpCookie.setComment(persistentCookie.getComment());
      httpCookie.setCommentURL(persistentCookie.getCommentURL());
      httpCookie.setDiscard(persistentCookie.isDiscard());
      httpCookie.setMaxAge(persistentCookie.getMaxAge());
      httpCookie.setPath(persistentCookie.getPath());
      httpCookie.setPortlist(persistentCookie.getPortList());
      httpCookie.setSecure(persistentCookie.isSecure());

      return httpCookie;
    }
  }
}
