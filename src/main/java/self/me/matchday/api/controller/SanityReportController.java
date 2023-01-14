/*
 * Copyright (c) 2023.
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

package self.me.matchday.api.controller;

import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import self.me.matchday.api.service.SanityCheckService;
import self.me.matchday.api.service.SanityCheckService.SanityReport;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Controller
@RequestMapping("/sanity-report")
public class SanityReportController {

  private final SanityCheckService sanityCheckService;

  public SanityReportController(SanityCheckService sanityCheckService) {
    this.sanityCheckService = sanityCheckService;
  }

  @GetMapping(value = "/generate/html", produces = MediaType.TEXT_HTML_VALUE)
  public String generateSanityReport(@NotNull Model model) {
    final SanityReport report = sanityCheckService.createSanityReport();
    model.addAttribute("report", report);
    model.addAttribute("danglingLocatorIds", getDanglingLocatorIds(report));
    model.addAttribute("danglingPlaylistIds", getDanglingPlaylistIds(report));
    return "sanity_report";
  }

  private String getDanglingLocatorIds(@NotNull SanityReport report) {
    return report.getVideoSanityReport().getDanglingStreamLocators().stream()
        .map(VideoStreamLocator::getStreamLocatorId)
        .map(Object::toString)
        .collect(Collectors.joining(", "));
  }

  private String getDanglingPlaylistIds(@NotNull SanityReport report) {
    return report.getVideoSanityReport().getDanglingPlaylists().stream()
        .map(VideoStreamLocatorPlaylist::getId)
        .map(Object::toString)
        .collect(Collectors.joining(", "));
  }

  @GetMapping(value = "/generate/json", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public SanityReport generateSanityReportJson() {
    return sanityCheckService.createSanityReport();
  }
}
