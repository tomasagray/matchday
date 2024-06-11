package net.tomasbot.matchday.api.controller.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import net.tomasbot.matchday.model.video.Resolution;
import org.jetbrains.annotations.NotNull;

public class ResolutionDeserializer extends JsonDeserializer<Resolution> {

  @Override
  public Resolution deserialize(@NotNull JsonParser parser, DeserializationContext context)
      throws IOException {
    final String str = parser.getValueAsString();
    return Resolution.fromString(str);
  }
}
