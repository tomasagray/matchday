package self.me.matchday.api.controller.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.ApplicationSettings;

import java.io.IOException;

import static self.me.matchday.api.controller.converter.ApplicationSettingsSerializer.TYPE;

public class ApplicationSettingsDeserializer extends JsonDeserializer<ApplicationSettings> {

    @Override
    public ApplicationSettings deserialize(@NotNull JsonParser jsonParser, DeserializationContext ctx)
            throws IOException {
        final String data = jsonParser.readValueAsTree().toString();
        return self.me.matchday.util.JsonParser.fromJson(data, TYPE);
    }
}
