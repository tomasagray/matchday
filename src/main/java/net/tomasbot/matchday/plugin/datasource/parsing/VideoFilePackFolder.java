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

package net.tomasbot.matchday.plugin.datasource.parsing;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.tomasbot.matchday.model.video.PartIdentifier;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoFilePack;
import net.tomasbot.matchday.plugin.datasource.parsing.fabric.Folder;

public class VideoFilePackFolder<T extends VideoFile> extends Folder<T, VideoFilePack> {

  @Override
  public Supplier<VideoFilePack> identity() {
    return VideoFilePack::new;
  }

  @Override
  public BiConsumer<T, VideoFilePack> accumulator() {
    return (videoFile, filePack) -> filePack.put(videoFile);
  }

  @Override
  public BiPredicate<T, VideoFilePack> isAccumulatorFull() {
    return (videoFile, filePack) -> {
      final VideoFile lastPart = filePack.lastPart();
      if (lastPart == null) return false;
      final boolean packFull = filePack.size() >= PartIdentifier.values().length;
      final boolean alreadyHas = videoFile.compareTo(lastPart) <= 0;
      return packFull || alreadyHas;
    };
  }
}
