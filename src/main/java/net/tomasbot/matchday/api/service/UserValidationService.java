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

package net.tomasbot.matchday.api.service;

import java.util.regex.Pattern;
import net.tomasbot.matchday.model.FileServerUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class UserValidationService {

  public static final int MIN_PASSWORD_LEN = 5;
  // Validation patterns
  private static final String EMAIL_REGEX =
      "(?:[a-z\\d!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z\\d!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-"
          + "\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z\\d]"
          + "(?:[a-z\\d-]*[a-z\\d])?\\.)+[a-z\\d](?:[a-z\\d-]*[a-z\\d])?|\\[(?:(?:25[0-5]|2[0-4]\\d|[01]"
          + "?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?|[a-z\\d-]*[a-z\\d]:(?:[\\x01-\\x08"
          + "\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])";
  private static final String PASSWORD_REGEX =
      "^[a-zA-Z\\d,_\\-()!@#$%^&*=+{\\[}\\];:'\"<>/?~`]{8,}$";

  public void validateUserForLogin(@NotNull final FileServerUser user) {
    validateEmailAddress(user.getUsername());
    validatePassword(user.getPassword());
  }

  public void validateEmailAddress(final String email) {
    final Pattern emailPattern = Pattern.compile(EMAIL_REGEX);
    final boolean isValid = (email != null) && emailPattern.matcher(email).find();
    if (!isValid) {
      throw new FileServerLoginException("Username is invalid");
    }
  }

  public void validatePassword(final String password) {
    final Pattern passwordPattern = Pattern.compile(PASSWORD_REGEX);
    final boolean isValid =
        (password != null)
            && password.length() > MIN_PASSWORD_LEN
            && passwordPattern.matcher(password).find();
    if (!isValid) {
      throw new FileServerLoginException("Password is invalid");
    }
  }
}
