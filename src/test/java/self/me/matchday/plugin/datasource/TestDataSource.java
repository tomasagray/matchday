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

package self.me.matchday.plugin.datasource;

import lombok.Getter;
import self.me.matchday.model.DataSource;
import self.me.matchday.model.video.VideoSourceMetadataPatternKit;

import javax.persistence.Entity;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
public class TestDataSource extends DataSource {

  TestDataSource(URI uri, List<VideoSourceMetadataPatternKit> metadataPatterns, UUID pluginId) {
    super(uri, metadataPatterns, pluginId);
  }
}
