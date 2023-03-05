package self.me.matchday.db.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import self.me.matchday.model.video.VideoStreamingError;
import self.me.matchday.util.JsonParser;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.lang.reflect.Type;

@Converter
public class VideoStreamingErrorConverter implements AttributeConverter<VideoStreamingError, String> {

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
