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

import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.video.StreamJobState.JobStatus;
import self.me.matchday.model.video.TaskState;
import self.me.matchday.model.video.VideoStreamLocator;

import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Receives log emissions from FFMPEG, interprets them and updates the specified VideoStreamLocator
 * state accordingly.
 */
public class FFmpegLogAdapter implements Consumer<String> {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\s*Duration: ([\\d:.]*)");
    private static final Pattern LOG_LINE_PATTERN = Pattern.compile("(\\w+=\\s*[\\w:.\\-/]+)");
    private static final Pattern TIME_PATTERN = Pattern.compile("((?:[\\d.]+:)+[\\d.]+)");

    private final BiConsumer<VideoStreamLocator, TaskState> onUpdate;
    private final VideoStreamLocator streamLocator;
    private long streamDuration;

    public FFmpegLogAdapter(VideoStreamLocator streamLocator, BiConsumer<VideoStreamLocator, TaskState> onUpdate) {
        this.streamLocator = streamLocator;
        this.onUpdate = onUpdate;
    }

    /**
     * Find the stream time within a log line. Assumes Matcher.find() has already been
     * called at least once.
     *
     * @param matcher The pattern matcher for the log line
     * @return The number of milliseconds streamed thus far or 0 if unable to determine
     */
    private static long parseTime(@NotNull Matcher matcher) {
        do {
            String data = matcher.group();  // get the next match
            Matcher timeMatcher = TIME_PATTERN.matcher(data);
            if (timeMatcher.find()) {
                return parseLogTime(timeMatcher.group());
            }
        } while (matcher.find());
        return 0;
    }

    /**
     * Reads a time String and returns the number of milliseconds that time represents
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

    @Override
    public void accept(String data) {
        Matcher durationMatcher = DURATION_PATTERN.matcher(data);
        Matcher dataMatcher = LOG_LINE_PATTERN.matcher(data);

        if (durationMatcher.find()) {
            String duration = durationMatcher.group(1);
            streamDuration = parseLogTime(duration);
        } else if (streamDuration > 0 && dataMatcher.find()) {
            long progress = parseTime(dataMatcher);
            double completionRatio = (progress / (double) streamDuration);
            updateStreamLocatorState(completionRatio);
        }
    }

    private void updateStreamLocatorState(double completionRatio) {
        onUpdate.accept(streamLocator, new TaskState(JobStatus.STREAMING, completionRatio));
    }
}
