package self.me.matchday.api.controller.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.video.Resolution;

public class ResolutionDeserializer extends JsonDeserializer<Resolution> {

  @Override
  public Resolution deserialize(@NotNull JsonParser parser, DeserializationContext context)
      throws IOException {
    final String str = parser.getValueAsString();
    return Resolution.fromString(str);
  }
}
