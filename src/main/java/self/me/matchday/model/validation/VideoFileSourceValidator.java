package self.me.matchday.model.validation;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.InvalidEventException;
import self.me.matchday.api.service.InvalidVideoFileSourceException;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFile;
import self.me.matchday.model.video.VideoFilePack;
import self.me.matchday.model.video.VideoFileSource;

@Component
public class VideoFileSourceValidator implements EntityValidator<VideoFileSource> {

  private static final Logger logger = LogManager.getLogger(VideoFileSourceValidator.class);

  private static boolean isValidVideoFilePack(@NotNull VideoFilePack filePack) {
    final Map<PartIdentifier, VideoFile> files = filePack.allFiles();
    final boolean hasBothHalves =
        files.containsKey(PartIdentifier.FIRST_HALF)
            && files.containsKey(PartIdentifier.SECOND_HALF);
    final boolean isFullCoverage = files.containsKey(PartIdentifier.FULL_COVERAGE);
    return hasBothHalves || isFullCoverage;
  }

  @Override
  public void validateAll(@NotNull Iterable<? extends VideoFileSource> fileSources) {
    int validCount = 0;
    for (VideoFileSource fileSource : fileSources) {
      try {
        validate(fileSource);
        validCount++;
      } catch (Throwable e) {
        final String msg = e.getMessage();
        logger.trace("Invalid VideoFileSource: {}; {}", fileSource, msg);
        logger.error("VideoFileSource is invalid: {}", msg);
      }
    }
    if (validCount == 0) {
      throw new InvalidEventException("No valid VideoFileSources");
    }
  }

  @Override
  public void validate(@Nullable VideoFileSource fileSource) {
    if (fileSource == null) {
      throw new InvalidVideoFileSourceException("VideoFileSource is null");
    }
    int validFilePacks = 0;
    final List<VideoFilePack> filePacks = fileSource.getVideoFilePacks();
    for (VideoFilePack filePack : filePacks) {
      if (isValidVideoFilePack(filePack)) {
        validFilePacks++;
      }
    }
    if (validFilePacks == 0) {
      throw new InvalidVideoFileSourceException("No valid VideoFilePacks in VideoFileSource");
    }
  }

  @Override
  public void validateForUpdate(
      @NotNull VideoFileSource existing, @NotNull VideoFileSource updated) {
    // nothing to validate yet...
  }
}
