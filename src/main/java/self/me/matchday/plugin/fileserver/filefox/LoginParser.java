/*
 * Copyright (c) 2021.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.fileserver.filefox;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import self.me.matchday.model.SecureCookie;
import self.me.matchday.plugin.fileserver.FileServerUser;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Component
public class LoginParser {

  private final ConnectionManager connectionManager;
  private final FileFoxPluginProperties pluginProperties;
  private final URI loginUri;

  public LoginParser(
      @Autowired ConnectionManager connectionManager,
      @Autowired FileFoxPluginProperties pluginProperties)
      throws URISyntaxException {

    this.connectionManager = connectionManager;
    this.pluginProperties = pluginProperties;
    this.loginUri = pluginProperties.getLoginUrl().toURI();
  }

  public ClientResponse performLogin(@NotNull final FileServerUser user) {

    // Perform login
    final Map<String, String> loginData = getLoginData(user);
    final LinkedMultiValueMap<String, String> emptyCookies = new LinkedMultiValueMap<>();
    ClientResponse loginResponse = connectionManager.post(loginUri, emptyCookies, loginData);

    final HttpStatus statusCode = loginResponse.statusCode();
    // todo - follow redirect? do we need more cookies?
    if (statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
      // Extract cookies
      final MultiValueMap<String, ResponseCookie> loginCookies = loginResponse.cookies();
      final HashSet<SecureCookie> cookies = new HashSet<>();
      // Map to SecureCookie
      loginCookies.forEach(
          (name, cookieJar) -> {
            final Set<SecureCookie> collect =
                cookieJar.stream().map(SecureCookie::fromSpringCookie).collect(Collectors.toSet());
            cookies.addAll(collect);
          });
      // Correct status code
      loginResponse = ClientResponse.from(loginResponse).statusCode(HttpStatus.OK).build();
      user.setCookies(cookies);
    }
    return loginResponse;
  }

  @NotNull
  private Map<String, String> getLoginData(@NotNull FileServerUser user) {

    final String email = URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8);
    final String password = URLEncoder.encode(user.getPassword(), StandardCharsets.UTF_8);

    final Map<String, String> loginData = new LinkedHashMap<>();
    loginData.put("email", email);
    loginData.put("password", password);
    loginData.put("op", "login");
    loginData.put("redirect", "");
    loginData.put("rand", "");
    return loginData;
  }
}
