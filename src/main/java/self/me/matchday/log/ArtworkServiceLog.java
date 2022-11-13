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

package self.me.matchday.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.api.service.ArtworkService;
import self.me.matchday.model.Image;

@Aspect
public class ArtworkServiceLog {

  private static final Logger logger = LogManager.getLogger(ArtworkService.class);

  @Around("execution(* self.me.matchday.api.service.ArtworkService.fetchArtworkData(..))")
  public Object logFetchArtworkData(@NotNull ProceedingJoinPoint jp) throws Throwable {
    logger.info("Reading data from disk for Artwork: {}", jp.getArgs()[0]);
    final Object image = jp.proceed();
    logger.info("Read image: {}", image);
    return image;
  }

  @Around("execution(* self.me.matchday.api.service.ArtworkService.addArtworkToCollection(..))")
  public Object logAddCompetitionEmblem(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object[] args = jp.getArgs();
    final Object collection = args[0];
    final Image image = (Image) args[1];
    final int imageSize = image.getData().length;
    logger.info("Adding image with {} bytes to collection: {}", imageSize, collection);

    final Object modifiedCollection = jp.proceed();
    logger.info("ArtworkCollection is now: {}", modifiedCollection);
    return modifiedCollection;
  }

  @Before("execution(* self.me.matchday.api.service.ArtworkService.repairArtworkFilePaths(..))")
  public void logRepairArtworkPath(@NotNull JoinPoint jp) {
    logger.info("Repairing Artwork file paths for collection: {}", jp.getArgs());
  }

  @Before(
      "execution(* self.me.matchday.api.service.ArtworkService.deleteArtworkFromCollection(..))")
  public void logDeleteArtworkFromCollection(@NotNull JoinPoint jp) {
    final Object[] args = jp.getArgs();
    logger.info("Deleting Artwork: {} from collection: {}", args[1], args[0]);
  }

  @Around("execution(* self.me.matchday.api.service.ArtworkService.deleteArtwork(..))")
  public Object logDeleteArtwork(@NotNull ProceedingJoinPoint jp) throws Throwable {
    final Object artwork = jp.getArgs()[0];
    logger.info("Attempting to delete Artwork: {}", artwork);
    final Object success = jp.proceed();
    logger.info("Successfully deleted artwork: {} ? {}", artwork, success);
    return success;
  }

  @Before("execution(* self.me.matchday.api.service.ArtworkService.deleteArtworkCollection(..))")
  public void logDeleteArtworkCollection(@NotNull JoinPoint jp) {
    logger.info("Deleting ArtworkCollection: {}", jp.getArgs()[0]);
  }
}
