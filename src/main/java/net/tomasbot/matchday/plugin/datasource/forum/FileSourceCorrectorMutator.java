package net.tomasbot.matchday.plugin.datasource.forum;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoFilePack;
import net.tomasbot.matchday.model.video.VideoFileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class FileSourceCorrectorMutator {

  private static final Pattern EXT_PATTERN = Pattern.compile("\\.(\\w+)$");
  private static final String DEFAULT_CONTAINER = "MKV";
  private static final String DEFAULT_DURATION = "90min";
  private static final String DEFAULT_LANGUAGE = "English";
  private static final String DEFAULT_VIDEO_CODEC = "H.264";
  private static final String DEFAULT_AUDIO_CODEC = "AAC";

  /**
   * Attempt to guess the media container (file extension) of the video files for this source from
   * one of the URLs of the supplied video source
   *
   * @param fileSource A video source
   * @return A trimmed, all-caps String representing the file extension, if it can be determined, or
   *     null if not
   */
  @Nullable
  private static String getExtensionFrom(@NotNull VideoFileSource fileSource) {
    return fileSource.getVideoFilePacks().stream()
        .map(VideoFilePack::allFiles)
        .flatMap(pack -> pack.values().stream())
        .map(VideoFile::getExternalUrl)
        .filter(Objects::nonNull)
        .findFirst()
        .map(
            url -> {
              Matcher matcher = EXT_PATTERN.matcher(url.toString());
              return matcher.find() ? matcher.group(1).trim().toUpperCase() : null;
            })
        .orElse(null);
  }

  private static void fixMediaContainer(@NotNull VideoFileSource fileSource) {
    final String mediaContainer = fileSource.getMediaContainer();
    if (mediaContainer == null || mediaContainer.isEmpty()) {
      String extension = getExtensionFrom(fileSource);
      fileSource.setMediaContainer(Objects.requireNonNullElse(extension, DEFAULT_CONTAINER));
    }

    // ensure all-caps
    fileSource.setMediaContainer(fileSource.getMediaContainer().toUpperCase());
  }

  private static void fixDuration(@NotNull VideoFileSource fileSource) {
    final String duration = fileSource.getApproximateDuration();
    if (duration == null || duration.isEmpty()) fileSource.setApproximateDuration(DEFAULT_DURATION);
  }

  private static void fixLanguages(@NotNull VideoFileSource fileSource) {
    // fix language
    final String languages = fileSource.getLanguages();
    if (languages == null || languages.isEmpty()) {
      fileSource.setLanguages(DEFAULT_LANGUAGE);
    } else {
      // ensure capitalization
      String capitalized = languages.substring(0, 1).toUpperCase() + languages.substring(1);
      fileSource.setLanguages(capitalized);
    }
  }

  private static void fixVideoCodec(@NotNull VideoFileSource fileSource) {
    final String videoCodec = fileSource.getVideoCodec();
    if (videoCodec == null || videoCodec.isEmpty()) {
      fileSource.setVideoCodec(DEFAULT_VIDEO_CODEC);
    }
  }

  private static void fixAudioCodec(@NotNull VideoFileSource fileSource) {
    final String audioCodec = fileSource.getAudioCodec();
    if (audioCodec == null || audioCodec.isEmpty()) {
      fileSource.setAudioCodec(DEFAULT_AUDIO_CODEC);
    }
  }

  public void correctFileSource(@NotNull VideoFileSource fileSource) {
    fixMediaContainer(fileSource);
    fixDuration(fileSource);
    fixLanguages(fileSource);
    fixVideoCodec(fileSource);
    fixAudioCodec(fileSource);
  }
}
