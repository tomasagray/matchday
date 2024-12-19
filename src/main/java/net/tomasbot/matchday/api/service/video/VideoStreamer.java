package net.tomasbot.matchday.api.service.video;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.tomasbot.ffmpeg_wrapper.request.SimpleTranscodeRequest;
import net.tomasbot.ffmpeg_wrapper.task.FFmpegStreamTask;
import net.tomasbot.matchday.model.video.StreamJobState.JobStatus;
import net.tomasbot.matchday.model.video.TaskState;
import net.tomasbot.matchday.model.video.VideoFile;
import net.tomasbot.matchday.model.video.VideoStreamLocator;
import net.tomasbot.matchday.model.video.VideoStreamingError;
import net.tomasbot.matchday.plugin.io.ffmpeg.FFmpegPlugin;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class VideoStreamer {

  private static final int STREAM_DELAY_MS = 50;

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
      // #############################################################################
      // TODO:
      // This is hacky and should not be necessary; it is done to avoid race condition
      // which causes duplicate VideoStreamLocators to be created in the database.
      // #############################################################################
      TimeUnit.MILLISECONDS.sleep(STREAM_DELAY_MS);

      final Long locatorId = streamLocator.getStreamLocatorId();
      final VideoFile videoFile = streamLocator.getVideoFile();
      final Path playlistPath = streamLocator.getPlaylistPath();

      updateLocatorTaskState(streamLocator, new TaskState(JobStatus.STARTED, 0.0));
      VideoFile refreshedVideoFile = videoFileService.refreshVideoFile(videoFile, false);
      URI videoDataLink = refreshedVideoFile.getInternalUrl().toURI();
      updateLocatorTaskState(streamLocator, new TaskState(JobStatus.BUFFERING, 0.0));

      // start stream
      FFmpegLogAdapter logAdapter = new FFmpegLogAdapter();
      final SimpleTranscodeRequest transcodeRequest =
          SimpleTranscodeRequest.builder()
              .from(videoDataLink)
              .to(playlistPath)
              .onEvent(data -> handleLoggingEvent(data, streamLocator, logAdapter))
              .onError(e -> setLocatorErrorState(streamLocator, new IOException(e)))
              .onComplete(ec -> completeStream(streamLocator))
              .logFile(FFmpegStreamTask.getDefaultLogFile())
              .build();
      FFmpegStreamTask streamTask = ffmpegPlugin.streamUri(transcodeRequest);
      updateLocatorTaskState(streamLocator, new TaskState(JobStatus.STREAMING, 0.0));
      streamTask.start();
      
      return CompletableFuture.completedFuture(locatorId);
    } catch (Throwable e) {
      setLocatorErrorState(streamLocator, e);
      throw new VideoStreamingException(e);
    } finally {
      if (onComplete != null) onComplete.run();
    }
  }

  private void handleLoggingEvent(
      String event, VideoStreamLocator locator, @NotNull FFmpegLogAdapter logAdapter) {
    logAdapter.update(event);
    double completionRatio = logAdapter.getCompletionRatio();
    TaskState taskState = new TaskState(JobStatus.STREAMING, completionRatio);
    updateLocatorTaskState(locator, taskState);
  }

  private void completeStream(@NotNull VideoStreamLocator locator) {
    final JobStatus previousStatus =
        locatorService
            .getStreamLocator(locator.getStreamLocatorId())
            .map(VideoStreamLocator::getState)
            .map(TaskState::getStatus)
            .orElse(JobStatus.COMPLETED);
    if (previousStatus.compareTo(JobStatus.STOPPED) > 0) {
      TaskState state = new TaskState(JobStatus.COMPLETED, 1.0);
      updateLocatorTaskState(locator, state);
    }
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
