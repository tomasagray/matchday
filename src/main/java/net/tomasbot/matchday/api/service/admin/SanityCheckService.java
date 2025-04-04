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

package net.tomasbot.matchday.api.service.admin;


import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import net.tomasbot.matchday.db.SanityReportRepository;
import net.tomasbot.matchday.model.ArtworkSanityReport;
import net.tomasbot.matchday.model.SanityReport;
import net.tomasbot.matchday.model.VideoSanityReport;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SanityCheckService {

  private final ArtworkSanityService artworkSanityService;
  private final ArtworkHealingService artworkHealingService;
  private final VideoSanityService videoSanityService;
  private final VideoHealingService videoHealingService;
  private final SanityReportRepository reportRepository;
  private final SanityReportRepository sanityReportRepository;

  public SanityCheckService(
      ArtworkSanityService artworkSanityService,
      ArtworkHealingService artworkHealingService,
      VideoSanityService videoSanityService,
      VideoHealingService videoHealingService,
      SanityReportRepository reportRepository,
      SanityReportRepository sanityReportRepository) {
    this.artworkSanityService = artworkSanityService;
    this.artworkHealingService = artworkHealingService;
    this.videoSanityService = videoSanityService;
    this.videoHealingService = videoHealingService;
    this.reportRepository = reportRepository;
    this.sanityReportRepository = sanityReportRepository;
  }

  private static boolean getRequiresHealing(
      @NotNull ArtworkSanityReport artworkSanityReport,
      @NotNull VideoSanityReport videoSanityReport) {
    int errorCount =
        videoSanityReport.getDanglingPlaylists().size()
            + videoSanityReport.getDanglingStreamLocators().size()
            + artworkSanityReport.getDanglingDbEntries().size()
            + artworkSanityReport.getDanglingFiles().size();
    return errorCount > 0;
  }

  public SanityReport createSanityReport() {
    ArtworkSanityReport artworkSanityReport = artworkSanityService.createArtworkSanityReport();
    VideoSanityReport videoSanityReport = videoSanityService.createVideoSanityReport();
    boolean requiresHealing = getRequiresHealing(artworkSanityReport, videoSanityReport);

    SanityReport report =
        SanityReport.builder()
            .artworkSanityReport(artworkSanityReport)
            .videoSanityReport(videoSanityReport)
            .requiresHealing(requiresHealing)
            .timestamp(Timestamp.from(Instant.now()))
            .build();
    return reportRepository.save(report);
  }

  // TODO: make this asynchronous task, report status via WebSocket
  public SanityReport autoHealSystem(@NotNull UUID reportId) throws IOException {
    SanityReport report = sanityReportRepository.getReferenceById(reportId);

    ArtworkSanityReport healedArt =
        artworkHealingService.autoHealArtwork(report.getArtworkSanityReport());
    VideoSanityReport healedVideos =
        videoHealingService.autoHealVideos(report.getVideoSanityReport());

    boolean requiresHealing = getRequiresHealing(healedArt, healedVideos);
    if (requiresHealing) throw new IllegalStateException("Could not auto-heal system");

    return SanityReport.builder()
        .artworkSanityReport(healedArt)
        .videoSanityReport(healedVideos)
        .requiresHealing(false)
        .timestamp(Timestamp.from(Instant.now()))
        .build();
  }
}
