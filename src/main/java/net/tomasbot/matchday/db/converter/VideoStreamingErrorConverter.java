package net.tomasbot.matchday.db.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import java.lang.reflect.Type;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import net.tomasbot.matchday.model.video.VideoStreamingError;
import net.tomasbot.matchday.util.JsonParser;

@Converter
public class VideoStreamingErrorConverter
    implements AttributeConverter<VideoStreamingError, String> {

  private static final Type type = new TypeReference<VideoStreamingError>() {}.getType();

  @Override
  public String convertToDatabaseColumn(VideoStreamingError attribute) {
    return JsonParser.toJson(attribute, type);
  }

  @Override
  public VideoStreamingError convertToEntityAttribute(String dbData) {
    return JsonParser.fromJson(dbData, type);
  }
}
