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

package self.me.matchday;

import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.Event;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;

import java.util.stream.Collectors;

public class PrettyDataPrinter {

  public static String getPrintableEvent(@NotNull final Event event) {

    final String ev =
        String.format(
            "Competition: %s%nHome Team: %s%nAway Team: %s%nSeason: %s%nFixture: %s%nDate: %s",
            event.getCompetition(),
            event.getHomeTeam(),
            event.getAwayTeam(),
            event.getSeason(),
            event.getFixture(),
            event.getDate());
    final String fileSources =
        event.getFileSources().stream()
            .map(PrettyDataPrinter::getPrintableFileSource)
            .collect(Collectors.joining("\n"));

    final int fileSourceCount = event.getFileSources().size();
    return String.format(
        "Event:%n=============%n%s%n------------------- File Sources (%d) ------------------- %n%s%n",
        ev, fileSourceCount, fileSources);
  }

  public static String getPrintableFileSource(@NotNull VideoFileSource fileSource) {

    final String fileSrc =
        String.format(
            "\tChannel: %s%n\tSource: %s%n\tResolution: %s%n\tDuration: %s%n\tLanguages: %s%n"
                + "\tContainer: %s%n\tVideo Codec: %s%n\tAudio Codec: %s%n\tVideo Bitrate: %s%n"
                + "\tAudio Channels: %s%n\tFilesize: %s%n\tFramerate: %s%n\t",
            fileSource.getChannel(),
            fileSource.getSource(),
            fileSource.getResolution(),
            fileSource.getApproximateDuration(),
            fileSource.getLanguages(),
            fileSource.getMediaContainer(),
            fileSource.getVideoCodec(),
            fileSource.getAudioCodec(),
            fileSource.getVideoBitrate(),
            fileSource.getAudioChannels(),
            fileSource.getFileSize(),
            fileSource.getFrameRate());
    final String videoFiles =
        fileSource.getVideoFilePacks().stream()
            .map(PrettyDataPrinter::getPrintableVideos)
            .collect(Collectors.joining("\n"));
    return String.format("File Source:%n--------%n%s%n%s", fileSrc, videoFiles);
  }

  public static String getPrintableVideos(@NotNull VideoFilePack filePack) {

    final StringBuilder sb = new StringBuilder();
    filePack.stream().sorted().forEach((videoFile) -> sb.append(videoFile).append("\n"));
    return String.format("Video File Collection:%n%s%n", sb);
  }
}
