package self.me.matchday.db;

import com.google.gson.Gson;
import javax.persistence.AttributeConverter;
import org.springframework.stereotype.Component;
import self.me.matchday.model.VideoMetadata;

@Component
public class VideoMetadataConverter implements AttributeConverter<VideoMetadata, String> {

  // Single Gson instance for all conversions
  private final Gson gson = new Gson();

  @Override
  public String convertToDatabaseColumn(VideoMetadata attribute) {
    return gson.toJson(attribute);
  }

  @Override
  public VideoMetadata convertToEntityAttribute(String dbData) {
    return gson.fromJson(dbData, VideoMetadata.class);
  }
}
