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

import static net.tomasbot.matchday.config.settings.ArtworkStorageLocation.ARTWORK_LOCATION;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.tomasbot.matchday.api.service.ArtworkService;
import net.tomasbot.matchday.api.service.SettingsService;
import net.tomasbot.matchday.api.service.video.VideoStreamingService;
import net.tomasbot.matchday.model.Artwork;
import net.tomasbot.matchday.model.SanityReport;
import net.tomasbot.matchday.model.SanityReport.ArtworkSanityReport.ArtworkSanityReportBuilder;
import net.tomasbot.matchday.model.SanityReport.VideoSanityReport.VideoSanityReportBuilder;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamLocatorPlaylist;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class SanityCheckService {

  private final ArtworkService artworkService;
  private final VideoStreamingService videoStreamingService;
  private final SettingsService settingsService;

  public SanityCheckService(
      ArtworkService artworkService,
      VideoStreamingService videoStreamingService,
      SettingsService settingsService) {
    this.artworkService = artworkService;
    this.videoStreamingService = videoStreamingService;
    this.settingsService = settingsService;
  }

  private static boolean getRequiresHealing(
      SanityReport.@NotNull ArtworkSanityReport artworkSanityReport,
      SanityReport.@NotNull VideoSanityReport videoSanityReport) {
    int errorCount =
        videoSanityReport.getDanglingPlaylists().size()
            + videoSanityReport.getDanglingStreamLocators().size()
            + artworkSanityReport.getDanglingDbEntries().size()
            + artworkSanityReport.getDanglingFiles().size();
    return errorCount > 0;
  }

  public SanityReport createSanityReport() {
    SanityReport.ArtworkSanityReport artworkSanityReport = createArtworkSanityReport();
    SanityReport.VideoSanityReport videoSanityReport = createVideoSanityReport();
    boolean requiresHealing = getRequiresHealing(artworkSanityReport, videoSanityReport);
    return SanityReport.builder()
        .artworkSanityReport(artworkSanityReport)
        .videoSanityReport(videoSanityReport)
        .requiresHealing(requiresHealing)
        .timestamp(Timestamp.from(Instant.now()))
        .build();
  }

  public SanityReport.ArtworkSanityReport createArtworkSanityReport() {
    final ArtworkSanityReportBuilder reportBuilder = SanityReport.ArtworkSanityReport.builder();
    return findDanglingArtworkFiles(findDanglingEntries(reportBuilder)).build();
  }

  public SanityReport.VideoSanityReport createVideoSanityReport() {
    final VideoSanityReportBuilder reportBuilder = SanityReport.VideoSanityReport.builder();
    return findDanglingLocatorPlaylists(findDanglingVideoStreamLocators(reportBuilder)).build();
  }

  // TODO: make this asynchronous task, report status via WebSocket
  public SanityReport autoHealSystem(@NotNull SanityReport report) throws IOException {
    SanityReport.ArtworkSanityReport healedArt = autoHealArtwork(report.getArtworkSanityReport());
    SanityReport.VideoSanityReport healedVideos = autoHealVideos(report.getVideoSanityReport());
    boolean requiresHealing = getRequiresHealing(healedArt, healedVideos);
    return SanityReport.builder()
        .artworkSanityReport(healedArt)
        .videoSanityReport(healedVideos)
        .requiresHealing(requiresHealing)
        .timestamp(Timestamp.from(Instant.now()))
        .build();
  }

  public SanityReport.ArtworkSanityReport autoHealArtwork(
      @NotNull SanityReport.ArtworkSanityReport report) throws IOException {
    List<Artwork> danglingDbEntries = report.getDanglingDbEntries();
    for (Artwork artwork : danglingDbEntries) {
      artworkService.deleteArtwork(artwork);
    }

    List<Path> danglingFiles = report.getDanglingFiles();
    deleteArtworkFiles(danglingFiles);
    return createArtworkSanityReport();
  }

  public void deleteArtworkFiles(@NotNull List<Path> danglingFiles) throws IOException {
    for (Path art : danglingFiles) {
      File artFile = art.toFile();
      if (!artFile.exists()) continue;
      boolean deleted = artFile.delete();
      if (!deleted || artFile.exists()) {
        throw new IOException("Could not delete Artwork file at: " + art);
      }
    }
  }

  public SanityReport.VideoSanityReport autoHealVideos(
      @NotNull SanityReport.VideoSanityReport report) throws IOException {
    List<? extends VideoStreamLocator> danglingStreamLocators = report.getDanglingStreamLocators();
    for (VideoStreamLocator locator : danglingStreamLocators) {
      videoStreamingService.deleteVideoStreamLocator(locator);
    }

    List<VideoStreamLocatorPlaylist> danglingPlaylists = report.getDanglingPlaylists();
    for (VideoStreamLocatorPlaylist playlist : danglingPlaylists) {
      videoStreamingService.deleteAllVideoData(playlist);
    }

    return createVideoSanityReport();
  }

  /**
   * Finds files which reside in the Artwork storage path, but do not have a corresponding entry in
   * the database.
   *
   * @param reportBuilder An instance of the ArtworkReportBuilder class
   * @return An updated report builder
   */
  @Contract("_ -> param1")
  private @NotNull ArtworkSanityReportBuilder findDanglingArtworkFiles(
      @NotNull ArtworkSanityReportBuilder reportBuilder) {
    final List<Path> danglingFiles = new ArrayList<>();
    // find all Artwork files
    final File storage = settingsService.getSetting(ARTWORK_LOCATION, Path.class).toFile();
    final File[] artworkFiles = storage.listFiles();
    if (artworkFiles != null) {
      // - save total files found
      reportBuilder.totalFiles(artworkFiles.length);
      for (final File file : artworkFiles) {
        final Path filepath = file.toPath();
        final Optional<Artwork> artwork = artworkService.fetchArtworkAt(filepath);
        if (artwork.isEmpty()) {
          // artwork not in DB
          danglingFiles.add(filepath);
        }
      }
    }
    reportBuilder.danglingFiles(danglingFiles);
    return reportBuilder;
  }

  /**
   * Check if there are Artwork entries in the database which do not have a file on the filesystem
   *
   * @param reportBuilder An instance of an ArtworkSanityReportBuilder
   * @return The updated report builder
   */
  @Contract("_ -> param1")
  private @NotNull ArtworkSanityReportBuilder findDanglingEntries(
      @NotNull ArtworkSanityReportBuilder reportBuilder) {
    final List<Artwork> danglingEntries = new ArrayList<>();
    final List<Artwork> artworks = artworkService.fetchAllArtwork();
    // - add total Artwork count to report
    reportBuilder.totalDbEntries(artworks.size());
    // check if each Artwork file exists
    for (final Artwork artwork : artworks) {
      final Path artworkFile = artwork.getFile();
      if (artworkFile == null || !artworkFile.toFile().exists()) {
        danglingEntries.add(artwork);
      }
    }
    // - add dangling DB entries
    reportBuilder.danglingDbEntries(danglingEntries);
    return reportBuilder;
  }

  /**
   * Find any VideoStreamLocators which do not have a corresponding playlist file on the filesystem.
   *
   * @param reportBuilder An instance of a VideoSanityReportBuilder
   * @return The updated report builder
   */
  @Contract("_ -> param1")
  private @NotNull VideoSanityReportBuilder findDanglingVideoStreamLocators(
      @NotNull VideoSanityReportBuilder reportBuilder) {
    final List<SanityReport.DanglingVideoStreamLocator> danglingLocators = new ArrayList<>();
    final List<VideoStreamLocator> streamLocators =
        videoStreamingService.fetchAllVideoStreamLocators();
    // - save total stream locators in database
    reportBuilder.totalStreamLocators(streamLocators.size());
    // check existence of playlist for each locator
    for (final VideoStreamLocator locator : streamLocators) {
      final Path playlistPath = locator.getPlaylistPath();
      if (!playlistPath.toFile().exists()) {
        SanityReport.DanglingVideoStreamLocator dangler =
            new SanityReport.DanglingVideoStreamLocator(locator);
        danglingLocators.add(dangler);
      }
    }
    // - add dangling locators
    reportBuilder.danglingStreamLocators(danglingLocators);
    return reportBuilder;
  }

  /**
   * Find any VideoStreamLocatorPlaylists which refer to non-existent storage locations.
   *
   * @param reportBuilder An instance of a VideoSanityReportBuilder
   * @return the updated report
   */
  @Contract("_ -> param1")
  private @NotNull VideoSanityReportBuilder findDanglingLocatorPlaylists(
      @NotNull VideoSanityReportBuilder reportBuilder) {
    final List<VideoStreamLocatorPlaylist> danglingPlaylists = new ArrayList<>();
    final List<VideoStreamLocatorPlaylist> playlists = videoStreamingService.fetchAllPlaylists();
    // - save total count of playlists
    reportBuilder.totalLocatorPlaylists(playlists.size());
    // check for existence of video storage location
    for (final VideoStreamLocatorPlaylist playlist : playlists) {
      final Path storageLocation = playlist.getStorageLocation();
      if (!storageLocation.toFile().exists()) {
        danglingPlaylists.add(playlist);
      }
    }
    // - add dangling playlists
    reportBuilder.danglingPlaylists(danglingPlaylists);
    return reportBuilder;
  }
}
