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

package self.me.matchday.api.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.net.URL;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;
import self.me.matchday.api.controller.VideoStreamingController;
import self.me.matchday.model.video.VideoFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonRootName(value = "video-file")
@Relation(collectionRelation = "video-files", itemRelation = "video-file")
@JsonInclude(value = Include.NON_NULL)
public class VideoFileResource extends RepresentationModel<VideoFileSourceResource> {

  private UUID videoFileId;
  private URL externalUrl;
  private Timestamp lastRefresh;
  private String title;

  @Component
  public static class VideoFileResourceModeller
      extends RepresentationModelAssemblerSupport<VideoFile, VideoFileResource> {

    public VideoFileResourceModeller() {
      super(VideoStreamingController.class, VideoFileResource.class);
    }

    @Override
    public @NotNull VideoFileResource toModel(@NotNull VideoFile entity) {
      final VideoFileResource model = instantiateModel(entity);
      model.setVideoFileId(entity.getFileId());
      model.setExternalUrl(entity.getExternalUrl());
      model.setLastRefresh(entity.getLastRefreshed());
      model.setTitle(entity.getTitle().getPartName());
      return model;
    }
  }
}
