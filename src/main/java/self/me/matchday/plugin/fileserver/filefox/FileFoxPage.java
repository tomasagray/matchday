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

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.URL;
import java.util.Map;

public abstract class FileFoxPage {

  abstract boolean isLoggedIn();

  abstract boolean isPremium();

  @EqualsAndHashCode(callSuper = true)
  @Data
  @Builder
  public static class DownloadLanding extends FileFoxPage {

    private final boolean premium;
    private final boolean loggedIn;
    private final Map<String, String> hiddenQueryParams;
    private final URI ddlSubmitUri;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  @Builder
  public static class DirectDownload extends FileFoxPage {

    private final boolean premium = true;
    private final boolean loggedIn = true;
    private final URL ddlUrl;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class Login extends FileFoxPage {

    private final boolean premium = false;
    private final boolean loggedIn = false;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  @Builder
  public static class Profile extends FileFoxPage {

    private final boolean premium;
    private final boolean loggedIn = true;
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  @NoArgsConstructor
  public static class Invalid extends FileFoxPage {
    private final boolean loggedIn = false;
    private final boolean premium = false;
    private String error = "Could not parse FileFox page";
  }
}
