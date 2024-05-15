package self.me.matchday.api.controller.converter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;
import self.me.matchday.model.ApplicationSettings;
import self.me.matchday.util.JsonParser;

import java.io.IOException;
import java.lang.reflect.Type;

public class ApplicationSettingsSerializer extends JsonSerializer<ApplicationSettings> {

    static final Type TYPE = new TypeReference<ApplicationSettings>() {
    }.getType();

    @Override
    public void serialize(ApplicationSettings settings, @NotNull JsonGenerator gen, SerializerProvider sp)
            throws IOException {
        final String json = JsonParser.toJson(settings, TYPE);
        gen.writeRaw(json);
    }
}
