package self.me.matchday.db.converter;

import java.nio.file.Path;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class PathConverter implements AttributeConverter<Path, String> {

  @Override
  public String convertToDatabaseColumn(Path attribute) {
    return attribute == null ? null : attribute.toString();
  }

  @Override
  public Path convertToEntityAttribute(String dbData) {
    return dbData == null ? null : Path.of(dbData);
  }
}
