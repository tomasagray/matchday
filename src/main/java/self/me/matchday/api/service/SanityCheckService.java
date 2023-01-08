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

package self.me.matchday.api.service;

import java.io.File;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import self.me.matchday.api.service.SanityCheckService.SanityReport.ArtworkSanityReport;
import self.me.matchday.api.service.SanityCheckService.SanityReport.ArtworkSanityReport.ArtworkSanityReportBuilder;
import self.me.matchday.api.service.SanityCheckService.SanityReport.VideoSanityReport;
import self.me.matchday.api.service.SanityCheckService.SanityReport.VideoSanityReport.VideoSanityReportBuilder;
import self.me.matchday.api.service.video.VideoStreamingService;
import self.me.matchday.model.Artwork;
import self.me.matchday.model.video.VideoStreamLocator;
import self.me.matchday.model.video.VideoStreamLocatorPlaylist;

@Service
public class SanityCheckService {

  private final ArtworkService artworkService;
  private final VideoStreamingService videoStreamingService;

  @Value("${artwork.storage-location}")
  private String artworkStorageLocation;

  public SanityCheckService(
      ArtworkService artworkService, VideoStreamingService videoStreamingService) {
    this.artworkService = artworkService;
    this.videoStreamingService = videoStreamingService;
  }

  public SanityReport createSanityReport() {
    return SanityReport.builder()
        .artworkSanityReport(createArtworkSanityReport())
        .videoSanityReport(createVideoSanityReport())
        .timestamp(Timestamp.from(Instant.now()))
        .build();
  }

  public ArtworkSanityReport createArtworkSanityReport() {
    final ArtworkSanityReportBuilder reportBuilder = ArtworkSanityReport.builder();
    return findDanglingArtworkFiles(findDanglingEntries(reportBuilder)).build();
  }

  public VideoSanityReport createVideoSanityReport() {
    final VideoSanityReportBuilder reportBuilder = VideoSanityReport.builder();
    return findDanglingLocatorPlaylists(findDanglingVideoStreamLocators(reportBuilder)).build();
  }

  /**
   * Finds files which reside in the Artwork storage path (env variable
   * ${artwork.storage-location}), but do not have a corresponding entry in the database.
   *
   * @param reportBuilder An instance of the ArtworkReportBuilder class
   * @return An updated report builder
   */
  private ArtworkSanityReportBuilder findDanglingArtworkFiles(
      @NotNull ArtworkSanityReportBuilder reportBuilder) {

    final List<Path> danglingFiles = new ArrayList<>();
    // find all Artwork files
    final File storage = new File(artworkStorageLocation);
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
      // - add "dangling" files
      reportBuilder.dangingFiles(danglingFiles);
    }
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
      if (!artworkFile.toFile().exists()) {
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
  private @NotNull VideoSanityReportBuilder findDanglingVideoStreamLocators(
      @NotNull VideoSanityReportBuilder reportBuilder) {

    final List<VideoStreamLocator> danglingLocators = new ArrayList<>();
    final List<VideoStreamLocator> streamLocators =
        videoStreamingService.fetchAllVideoStreamLocators();
    // - save total stream locators in database
    reportBuilder.totalStreamLocators(streamLocators.size());
    // check existence of playlist for each locator
    for (final VideoStreamLocator locator : streamLocators) {
      final Path playlistPath = locator.getPlaylistPath();
      if (!playlistPath.toFile().exists()) {
        danglingLocators.add(locator);
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

  @Data
  @Builder
  public static final class SanityReport {

    private final ArtworkSanityReport artworkSanityReport;
    private final VideoSanityReport videoSanityReport;
    private final Timestamp timestamp;

    @Data
    @Builder
    public static final class ArtworkSanityReport {
      private final List<Path> dangingFiles;
      private final List<Artwork> danglingDbEntries;
      private long totalFiles;
      private long totalDbEntries;
    }

    @Data
    @Builder
    public static final class VideoSanityReport {
      private final List<VideoStreamLocator> danglingStreamLocators;
      private final List<VideoStreamLocatorPlaylist> danglingPlaylists;
      private long totalStreamLocators;
      private long totalLocatorPlaylists;
    }
  }
}
