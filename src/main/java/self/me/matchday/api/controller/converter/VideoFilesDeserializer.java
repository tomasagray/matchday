package self.me.matchday.api.controller.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.video.PartIdentifier;
import self.me.matchday.model.video.VideoFile;

public class VideoFilesDeserializer extends JsonDeserializer<Map<PartIdentifier, VideoFile>> {

  private static @NotNull VideoFile parseVideoFile(@NotNull TreeNode subNode)
      throws MalformedURLException {
    final VideoFile videoFile = new VideoFile();
    for (Iterator<String> yt = subNode.fieldNames(); yt.hasNext(); ) {
      final String fieldName = yt.next();
      final String fieldVal = subNode.get(fieldName).toString();
      if ("null".equals(fieldVal)) {
        continue;
      }
      final String value = fieldVal.substring(1, fieldVal.length() - 1); // remove "'s
      switch (fieldName) {
        case "videoFileId" -> videoFile.setFileId(UUID.fromString(value));
        case "externalUrl" -> videoFile.setExternalUrl(new URL(value));
        case "title" -> videoFile.setTitle(PartIdentifier.from(value));
      }
    }
    return videoFile;
  }

  @Override
  public Map<PartIdentifier, VideoFile> deserialize(
      @NotNull JsonParser p, DeserializationContext ctxt) throws IOException {

    final Map<PartIdentifier, VideoFile> videoFiles = new HashMap<>();
    final TreeNode node = p.getCodec().readTree(p);
    for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
      final String partValue = it.next();
      final PartIdentifier part = PartIdentifier.valueOf(partValue);
      final TreeNode subNode = node.get(partValue);
      final VideoFile videoFile = parseVideoFile(subNode);
      videoFiles.put(part, videoFile);
    }
    return videoFiles;
  }
}
