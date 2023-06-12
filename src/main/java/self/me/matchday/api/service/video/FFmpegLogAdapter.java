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

package self.me.matchday.api.service.video;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.video.StreamJobState.JobStatus;
import self.me.matchday.model.video.VideoStreamLocator;

/**
 * Receives log emissions from FFMPEG, interprets them and updates the specified VideoStreamLocator
 * state accordingly.
 */
public class FFmpegLogAdapter implements Consumer<String> {

  private static final Pattern durationPattern = Pattern.compile("^\\s*Duration: ([\\d:.]*)");
  private static final Pattern timeProgressPattern =
      Pattern.compile(
          "frame=(\\s*\\d+) fps=(\\s*[\\d.]+) q=([\\d-.]*) size=([\\w/]*) time=([\\d:.]*) bitrate=([\\w/])* speed=([\\d.]*x)");

  private final VideoStreamLocatorService locatorService;
  private final VideoStreamLocator streamLocator;
  private long streamDuration;

  public FFmpegLogAdapter(
      @NotNull final VideoStreamLocatorService locatorService,
      @NotNull final VideoStreamLocator streamLocator) {
    this.locatorService = locatorService;
    this.streamLocator = streamLocator;
  }

  @Override
  public void accept(String data) {

    // compute job % done
    final Matcher durationMatcher = durationPattern.matcher(data);
    final Matcher timeProgressMatcher = timeProgressPattern.matcher(data);

    // if it's the "duration" line
    if (durationMatcher.find()) {
      final String duration = durationMatcher.group(1);
      streamDuration = parseLogTime(duration);
    } else if (timeProgressMatcher.find()) {
      final String time = timeProgressMatcher.group(5);
      final long progress = parseLogTime(time);
      final double completionRatio = (progress / (double) streamDuration);
      updateStreamLocatorState(completionRatio);
    }
  }

  private long parseLogTime(@NotNull final String data) {

    final Pattern pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})([\\d.]*)");
    final Matcher matcher = pattern.matcher(data);

    if (matcher.find()) {
      final long hours = Integer.parseInt(matcher.group(1));
      final long mins = Integer.parseInt(matcher.group(2));
      final long secs = Integer.parseInt(matcher.group(3));
      final String nanoStr = matcher.group(4);
      long nanos = 0;
      if (!nanoStr.isEmpty()) {
        nanos = (long) (Double.parseDouble(nanoStr) * 1_000d);
      }
      return (hours * 60 * 60 * 1_000) + (mins * 60 * 1_000) + (secs * 1_000) + nanos;
    }
    return 0;
  }

  private void updateStreamLocatorState(final double completionRatio) {
    streamLocator.updateState(JobStatus.STREAMING, completionRatio);
    locatorService.updateStreamLocator(streamLocator);
  }
}
