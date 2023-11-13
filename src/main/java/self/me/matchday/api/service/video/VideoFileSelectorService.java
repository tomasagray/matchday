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

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.FileServerPluginService;
import self.me.matchday.model.Country;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;

@Service
public class VideoFileSelectorService {

  private final FileServerPluginService pluginService;

  public VideoFileSelectorService(FileServerPluginService pluginService) {
    this.pluginService = pluginService;
  }

  private static String getSourcePrimaryLanguage(@NotNull VideoFileSource source) {
    final String languages = source.getLanguages();
    if (languages.contains(" ")) {
      return languages.split("\\s")[0];
    }
    return languages.trim();
  }

  /**
   * Get the "best" file source, sorted by: language -> bitrate -> resolution
   *
   * @param event The event containing the file sources
   * @return The "best" file source
   */
  public VideoFileSource getBestFileSource(@NotNull final Event event) {

    // get a mutable copy
    final List<VideoFileSource> sortedFileSources = new ArrayList<>(event.getFileSources());
    // sort file sources by:
    sortedFileSources.sort(
        // - resolution
        Comparator.comparing(VideoFileSource::getResolution)
            // - competition language
            .thenComparing(
                VideoFileSelectorService::getSourcePrimaryLanguage,
                (first, second) ->
                    compareLanguages(event.getCompetition().getCountry(), first, second))
            // - home team language
            .thenComparing(
                VideoFileSelectorService::getSourcePrimaryLanguage,
                (first, second) -> {
                  if (event instanceof final Match match) {
                    return compareLanguages(match.getHomeTeam().getCountry(), first, second);
                  }
                  return 0;
                })
            // - away team language
            .thenComparing(
                VideoFileSelectorService::getSourcePrimaryLanguage,
                (first, second) -> {
                  if (event instanceof final Match match) {
                    return compareLanguages(match.getAwayTeam().getCountry(), first, second);
                  }
                  return 0;
                }));
    // get top result
    return sortedFileSources.get(0);
  }

  private int compareLanguages(@Nullable Country country, String first, String second) {
    if (country == null) return 0;
    List<Locale> locales = country.getLocales();
    if (locales == null || locales.size() == 0) return 0;
    final Boolean firstMatches =
        locales.stream().map(Locale::getDisplayLanguage).anyMatch(locale -> locale.equals(first));
    final Boolean secondMatches =
        locales.stream().map(Locale::getDisplayLanguage).anyMatch(locale -> locale.equals(second));
    return secondMatches.compareTo(firstMatches);
  }

  /**
   * Get the best version of each VideoFile for this VideoFileSource, and return them in the correct
   * order.
   *
   * @param fileSource The source of video data for this Event
   * @return The "best" versions of each VideoFile
   */
  public VideoFilePack getPlaylistFiles(@NotNull final VideoFileSource fileSource) {

    // get mutable copy
    final List<VideoFilePack> sortedPacks = new ArrayList<>(fileSource.getVideoFilePacks());
    sortedPacks.sort(
        Comparator.comparing(
            pack -> {
              final VideoFile firstPart = pack.firstPart();
              if (firstPart != null) {
                final URL url = firstPart.getExternalUrl();
                return pluginService.getEnabledPluginForUrl(url);
              }
              return null;
            },
            (o1, o2) -> {
              final Boolean hasPlugin1 = o1 != null;
              final Boolean hasPlugin2 = o2 != null;
              return hasPlugin1.compareTo(hasPlugin2);
            }));
    return sortedPacks.get(0);
  }
}
