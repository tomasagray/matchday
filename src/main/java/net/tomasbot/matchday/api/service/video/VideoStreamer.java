package net.tomasbot.matchday.api.service.video;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.tomasbot.matchday.model.video.StreamJobState.JobStatus;
import net.tomasbot.matchday.model.video.TaskState;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamingError;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegLogger;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegStreamTask;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class VideoStreamer {

  private static final int STREAM_DELAY_MS = 50;
  private static final OpenOption[] LOG_WRITE_OPTIONS = {
    StandardOpenOption.CREATE, StandardOpenOption.WRITE
  };

  private final VideoFileService videoFileService;
  private final VideoStreamLocatorService locatorService;
  private final FFmpegPlugin ffmpegPlugin;

  public VideoStreamer(
      VideoFileService videoFileService,
      VideoStreamLocatorService locatorService,
      FFmpegPlugin ffmpegPlugin) {
    this.videoFileService = videoFileService;
    this.locatorService = locatorService;
    this.ffmpegPlugin = ffmpegPlugin;
  }

  @Async("VideoStreamExecutor")
  public CompletableFuture<Long> beginStreaming(
      @NotNull VideoStreamLocator streamLocator, Runnable onComplete) {
    try {
      final Long locatorId = streamLocator.getStreamLocatorId();
      final VideoFile videoFile = streamLocator.getVideoFile();
      final Path playlistPath = streamLocator.getPlaylistPath();

      // #############################################################################
      // TODO:
      // This is hacky and should not be necessary; it is done to avoid race condition
      // which causes duplicate VideoStreamLocators to be created in the database.
      // #############################################################################
      TimeUnit.MILLISECONDS.sleep(STREAM_DELAY_MS);

      updateLocatorTaskState(streamLocator, new TaskState(JobStatus.STARTED, 0.0));
      VideoFile refreshedVideoFile = videoFileService.refreshVideoFile(videoFile, false);
      URI videoDataLink = refreshedVideoFile.getInternalUrl().toURI();
      updateLocatorTaskState(streamLocator, new TaskState(JobStatus.BUFFERING, 0.0));

      // start stream
      FFmpegStreamTask streamTask = ffmpegPlugin.streamUris(playlistPath, videoDataLink);
      updateLocatorTaskState(streamLocator, new TaskState(JobStatus.STREAMING, 0.0));
      if (streamTask.isLoggingEnabled()) {
        streamWithLogging(streamLocator, streamTask);
        // ^ JobStatus updated to COMPLETED in above method
      } else {
        streamWithoutLog(streamTask);
        updateLocatorTaskState(streamLocator, new TaskState(JobStatus.COMPLETED, 1.0));
      }
      return CompletableFuture.completedFuture(locatorId);
    } catch (Throwable e) {
      setLocatorErrorState(streamLocator, e);
      throw new VideoStreamingException(e);
    } finally {
      if (onComplete != null) onComplete.run();
    }
  }

  private void streamWithLogging(
      @NotNull VideoStreamLocator streamLocator, @NotNull FFmpegStreamTask streamTask)
      throws IOException {
    Long locatorId = streamLocator.getStreamLocatorId();
    final Process streamProcess = streamTask.execute();

    final FFmpegLogger logger = new FFmpegLogger(streamProcess, streamTask.getDataDir());
    final FFmpegLogAdapter logReader =
        new FFmpegLogAdapter(streamLocator, this::updateLocatorTaskState);

    AsynchronousFileChannel fw =
        AsynchronousFileChannel.open(logger.getLogFile(), LOG_WRITE_OPTIONS);
    final Flux<String> logEmitter = logger.beginLogging(fw);
    logEmitter
        .doOnNext(logReader)
        .doOnComplete(
            () -> {
              JobStatus previousStatus =
                  locatorService
                      .getStreamLocator(locatorId)
                      .map(VideoStreamLocator::getState)
                      .map(TaskState::getStatus)
                      .orElse(JobStatus.COMPLETED);
              if (previousStatus.compareTo(JobStatus.STOPPED) > 0) {
                TaskState state = new TaskState(JobStatus.COMPLETED, 1.0);
                updateLocatorTaskState(streamLocator, state);
              }
            })
        .subscribe();
  }

  private void streamWithoutLog(@NotNull final FFmpegStreamTask streamTask)
      throws IOException, InterruptedException {
    final Process process = streamTask.execute();
    // absorb process output
    final InputStream errorStream = process.getErrorStream();
    final BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
    reader.lines().forEach(line -> {});
    // wait for process to finish
    process.waitFor();
    process.destroy();
  }

  public void updateLocatorTaskState(
      @NotNull VideoStreamLocator streamLocator, @NotNull TaskState state) {
    streamLocator.updateState(state.getStatus(), state.getCompletionRatio(), state.getError());
    locatorService.updateStreamLocator(streamLocator);
    locatorService.publishLocatorStatus(streamLocator);
  }

  private void setLocatorErrorState(
      @NotNull VideoStreamLocator streamLocator, @NotNull Throwable error) {
    final TaskState taskState = new TaskState();
    taskState.setError(new VideoStreamingError(error));
    taskState.setCompletionRatio(-1.0);
    taskState.setStatus(JobStatus.ERROR);
    updateLocatorTaskState(streamLocator, taskState);
  }
}
