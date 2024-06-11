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

package net.tomasbot.matchday.api.controller;

import static net.tomasbot.matchday.config.StatusWebSocketConfigurer.BROKER_ROOT;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import net.tomasbot.matchday.db.VideoStreamLocatorRepo;
import net.tomasbot.matchday.model.video.StreamJobState.JobStatus;
import net.tomasbot.matchday.model.video.TaskState;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamingError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class VideoStreamStatusController {

  public static final String RECEIVE_ENDPOINT = "/video-stream-status";
  public static final String VIDEO_STREAM_EMIT_ENDPOINT = BROKER_ROOT + "/video-stream-status";

  private final VideoStreamLocatorRepo locatorRepo;

  public VideoStreamStatusController(VideoStreamLocatorRepo locatorRepo) {
    this.locatorRepo = locatorRepo;
  }

  @MessageMapping(RECEIVE_ENDPOINT)
  @SendTo(VIDEO_STREAM_EMIT_ENDPOINT)
  public VideoStreamStatusMessage publishVideoStreamStatus(@NotNull UUID videoFileId) {
    final VideoStreamLocator streamLocator = getStreamLocatorFor(videoFileId);
    if (streamLocator != null) {
      final TaskState state = streamLocator.getState();
      final JobStatus status = state.getStatus();
      final Double completionRatio = state.getCompletionRatio();
      final VideoStreamingError error = state.getError();
      return VideoStreamStatusMessage.builder()
          .videoFileId(videoFileId)
          .status(status)
          .completionRatio(completionRatio)
          .error(error)
          .build();
    } else {
      return VideoStreamStatusMessage.builder()
          .videoFileId(videoFileId)
          .status(null)
          .completionRatio(0d)
          .build();
    }
  }

  private @Nullable VideoStreamLocator getStreamLocatorFor(@NotNull UUID videoFileId) {
    final List<VideoStreamLocator> locators = locatorRepo.getStreamLocatorsFor(videoFileId);
    if (!locators.isEmpty()) {
      return locators.get(0);
    } else {
      return null;
    }
  }

  @Data
  @Builder
  public static class VideoStreamStatusMessage {
    private final UUID videoFileId;
    private final JobStatus status;
    private final Double completionRatio;
    private VideoStreamingError error;
  }
}
