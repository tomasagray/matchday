package net.tomasbot.matchday.api.controller.converter;

import static net.tomasbot.matchday.api.controller.converter.ApplicationSettingsSerializer.TYPE;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import net.tomasbot.matchday.model.ApplicationSettings;
import org.jetbrains.annotations.NotNull;

public class ApplicationSettingsDeserializer extends JsonDeserializer<ApplicationSettings> {

  @Override
  public ApplicationSettings deserialize(@NotNull JsonParser jsonParser, DeserializationContext ctx)
      throws IOException {
    final String data = jsonParser.readValueAsTree().toString();
    return net.tomasbot.matchday.util.JsonParser.fromJson(data, TYPE);
  }
}
