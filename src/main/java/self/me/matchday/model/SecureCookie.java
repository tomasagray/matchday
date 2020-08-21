package self.me.matchday.model;

import java.time.Duration;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;

/**
 * Wraps Spring's HttpCookie & ResponseCookie classes for JPA persistence
 */
@Entity
@Data
public class SecureCookie {

  @Id
  @GeneratedValue
  private Long id;
  // HttpCookie
  private final String name;
  private final String value;
  // ResponseCookie
  @Nullable
  private Duration maxAge;
  @Nullable
  private String domain;
  @Nullable
  private String path;
  private boolean secure;
  private boolean httpOnly;
  @Nullable
  private String sameSite;

  public SecureCookie() {
    this.name = "null";
    this.value = "null";
  }

  public SecureCookie(@NotNull final String name, @NotNull final String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Map a Spring HttpCookie to a SecureCookie for encryption & persistence
   *
   * @param cookie The Spring cookie
   * @return A SecureCookie
   */
  public static SecureCookie fromSpringCookie(@NotNull final HttpCookie cookie) {

    // Create base instance
    final SecureCookie secureCookie = new SecureCookie(cookie.getName(), cookie.getValue());

    if (cookie instanceof ResponseCookie) {
      // Copy response cookie values
      final ResponseCookie responseCookie = (ResponseCookie) cookie;
      secureCookie.setMaxAge(responseCookie.getMaxAge());
      secureCookie.setDomain(responseCookie.getDomain());
      secureCookie.setPath(responseCookie.getPath());
      secureCookie.setSecure(responseCookie.isSecure());
      secureCookie.setHttpOnly(responseCookie.isHttpOnly());
      secureCookie.setSameSite(responseCookie.getSameSite());
    }

    return secureCookie;
  }

  public static HttpCookie toSpringCookie(@NotNull final SecureCookie secureCookie) {

    // If secure cookie is a response cookie
    if ((secureCookie.getMaxAge() != null) &&
        (secureCookie.getDomain() != null) &&
        (secureCookie.getPath() != null)) {

      // Create response cookie
      return
          ResponseCookie.from(secureCookie.getName(), secureCookie.getValue())
              .maxAge(secureCookie.getMaxAge())
              .domain(secureCookie.getDomain())
              .path(secureCookie.getPath())
              .secure(secureCookie.isSecure())
              .httpOnly(secureCookie.isHttpOnly())
              .sameSite(secureCookie.getSameSite())
              .build();

    } else {
      return
          new HttpCookie(secureCookie.getName(), secureCookie.getValue());
    }
  }
}
