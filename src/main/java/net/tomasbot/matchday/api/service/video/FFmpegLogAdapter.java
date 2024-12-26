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

package net.tomasbot.matchday.api.service.video;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Receives log emissions from FFMPEG, interprets them and updates the specified VideoStreamLocator
 * state accordingly.
 */
public class FFmpegLogAdapter {

  // todo - extract patterns
  private static final Pattern DURATION_PATTERN = Pattern.compile("^\\s*Duration: ([\\d:.]*)");
  private static final Pattern LOG_LINE_PATTERN = Pattern.compile("(\\w+=\\s*[\\w:.\\-/]+)");
  private static final Pattern TIME_PATTERN = Pattern.compile("((?:[\\d.]+:)+[\\d.]+)");

  private long streamDuration;
  @Getter private double completionRatio;

  /**
   * Find the stream time within a log line. Assumes Matcher.find() has already been called at least
   * once.
   *
   * @param matcher The pattern matcher for the log line
   * @return The number of milliseconds streamed thus far or 0 if unable to determine
   */
  private static long parseLogLine(@NotNull Matcher matcher) {
    do {
      String data = matcher.group(); // get the next match
      Matcher timeMatcher = TIME_PATTERN.matcher(data);
      if (timeMatcher.find()) {
        return parseLogTime(timeMatcher.group());
      }
    } while (matcher.find());
    return 0;
  }

  /**
   * Reads a time String and returns the number of milliseconds that time represents
   *
   * @param data A String representing a time, e.g., 00:13:43.32 or 00:00
   * @return The number of milliseconds for the given time
   */
  private static long parseLogTime(@NotNull final String data) {
    final Matcher matcher = TIME_PATTERN.matcher(data);
    if (matcher.find()) {
      LocalTime time = LocalTime.parse(matcher.group());
      return time.getLong(ChronoField.MILLI_OF_DAY);
    }
    return 0;
  }

  public void update(String data) {
    Matcher durationMatcher = DURATION_PATTERN.matcher(data);
    Matcher dataMatcher = LOG_LINE_PATTERN.matcher(data);

    if (durationMatcher.find()) {
      String duration = durationMatcher.group(1);
      this.streamDuration = parseLogTime(duration);
    } else if (streamDuration > 0 && dataMatcher.find()) {
      long progress = parseLogLine(dataMatcher);
      this.completionRatio = (progress / (double) streamDuration);
    }
  }
}
