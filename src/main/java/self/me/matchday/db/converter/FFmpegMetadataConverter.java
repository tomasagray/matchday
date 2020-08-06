package self.me.matchday.db.converter;

import com.google.gson.Gson;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import self.me.matchday.plugin.io.ffmpeg.FFmpegMetadata;

@Converter
public class FFmpegMetadataConverter implements AttributeConverter<FFmpegMetadata, String> {

  // Single Gson instance for all conversions
  private final Gson gson = new Gson();

  @Override
  public String convertToDatabaseColumn(FFmpegMetadata attribute) {
    return gson.toJson(attribute);
  }

  @Override
  public FFmpegMetadata convertToEntityAttribute(String dbData) {
    return gson.fromJson(dbData, FFmpegMetadata.class);
  }
}
