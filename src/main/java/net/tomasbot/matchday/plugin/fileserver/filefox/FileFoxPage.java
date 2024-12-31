/*
 * Copyright (c) 2022.
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

package net.tomasbot.matchday.plugin.fileserver.filefox;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public abstract class FileFoxPage {

  private final boolean premium;
  private final boolean loggedIn;
  private String text;

  @EqualsAndHashCode(callSuper = true)
  @Data
  @SuperBuilder
  public static class DownloadLanding extends FileFoxPage {

    private final Map<String, String> hiddenQueryParams;
    private final URI ddlSubmitUri;

    @Override
    public boolean isPremium() {
      return true;
    }

    @Override
    public boolean isLoggedIn() {
      return true;
    }
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  @SuperBuilder
  public static class DirectDownload extends FileFoxPage {
    private final URL ddlUrl;

    @Override
    public boolean isPremium() {
      return true;
    }

    @Override
    public boolean isLoggedIn() {
      return true;
    }
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  @SuperBuilder
  public static class Login extends FileFoxPage {
    @Override
    public boolean isPremium() {
      return false;
    }

    @Override
    public boolean isLoggedIn() {
      return false;
    }
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  @SuperBuilder
  public static class Profile extends FileFoxPage {
    private float trafficAvailable;

    @Override
    public boolean isLoggedIn() {
      return true;
    }
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  @SuperBuilder
  public static class Invalid extends FileFoxPage {

    private String error;

    @Override
    public boolean isPremium() {
      return false;
    }

    @Override
    public boolean isLoggedIn() {
      return false;
    }
  }
}
