package net.tomasbot.matchday.common.flaresolverr;

import java.net.URL;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.tomasbot.matchday.model.SecureCookie;

@Data
@AllArgsConstructor
public class FlaresolverrSolution {

  private URL url;
  private int status;
  private String data;
  private Set<SecureCookie> cookies;
}
