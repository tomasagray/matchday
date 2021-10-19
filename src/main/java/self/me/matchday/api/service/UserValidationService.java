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

package self.me.matchday.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.matchday.model.FileServerUser;
import self.me.matchday.util.Log;

import java.util.regex.Pattern;

@Service
public class UserValidationService {

  // Validation patterns
  private static final String EMAIL_REGEX =
      "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-"
          + "\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9]"
          + "(?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]"
          + "?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08"
          + "\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])";
  private static final String PASSWORD_REGEX =
      "^[a-zA-Z0-9,_\\-()!@#$%^&*=+{\\[}\\];:'\"<>/?~`]{8,}$";

  public boolean isValidUserData(@NotNull final FileServerUser user) {
    return isValidEmailAddress(user.getUsername()) && isValidPassword(user.getPassword());
  }

  public boolean isValidEmailAddress(final String email) {

    final Pattern emailPattern = Pattern.compile(EMAIL_REGEX);
    // Match email address
    final boolean isValid = (email != null) && emailPattern.matcher(email).find();
    Log.i("UserValidation", String.format("Email: %s is valid? %s", email, isValid));
    return isValid;
  }

  public boolean isValidPassword(final String password) {

    final Pattern passwordPattern = Pattern.compile(PASSWORD_REGEX);
    final boolean isValid = (password != null) && passwordPattern.matcher(password).find();
    Log.i("UserValidation", String.format("Password is valid? %s", isValid));
    return isValid;
  }
}
