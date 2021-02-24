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

public enum PageType {
    FreeDownloadLanding(false, true),
    PremiumDownloadLanding(true, true),
    DirectDownload(true, true),
    Login(false, false),
    PremiumProfile(true, true),
    FreeProfile(false, true),
    Invalid(false, false);

    private final boolean premium;
    private final boolean loggedIn;

    PageType(final boolean premium, final boolean loggedIn) {
        this.premium = premium;
        this.loggedIn = loggedIn;
    }

    public boolean isPremium() {
        return premium;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
