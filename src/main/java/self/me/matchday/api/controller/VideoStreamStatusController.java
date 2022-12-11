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

package self.me.matchday.api.controller;

import static self.me.matchday.config.VideoStatusWebConfigurer.BROKER_ROOT;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import self.me.matchday.db.VideoStreamLocatorRepo;
import self.me.matchday.model.video.StreamJobState.JobStatus;
import self.me.matchday.model.video.TaskState;
import self.me.matchday.model.video.VideoStreamLocator;

@Controller
public class VideoStreamStatusController {

  public static final String EMIT_ENDPOINT = BROKER_ROOT + "/video-stream";
  public static final String RECEIVE_ENDPOINT = "/status";
  private final VideoStreamLocatorRepo locatorRepo;

  public VideoStreamStatusController(VideoStreamLocatorRepo locatorRepo) {
    this.locatorRepo = locatorRepo;
  }

  @SendTo(EMIT_ENDPOINT)
  @MessageMapping(RECEIVE_ENDPOINT)
  @Transactional
  public VideoStreamStatusMessage publishVideoStreamStatus(@NotNull UUID videoFileId) {

    final VideoStreamLocator streamLocator = getStreamLocatorFor(videoFileId);
    if (streamLocator != null) {
      final TaskState state = streamLocator.getState();
      final JobStatus status = state.getStatus();
      final Double completionRatio = state.getCompletionRatio();
      return new VideoStreamStatusMessage(videoFileId, status, completionRatio);
    } else {
      return new VideoStreamStatusMessage(videoFileId, null, 0d);
    }
  }

  private @Nullable VideoStreamLocator getStreamLocatorFor(@NotNull UUID videoFileId) {
    final List<VideoStreamLocator> locators = locatorRepo.getStreamLocatorsFor(videoFileId);
    if (locators.size() > 0) {
      return locators.get(0);
    } else {
      return null;
    }
  }

  @Data
  public static class VideoStreamStatusMessage {
    private final UUID videoFileId;
    private final JobStatus status;
    private final Double completionRatio;
  }
}
