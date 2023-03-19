package self.me.matchday.model.validation;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;

@Component
public class ProperNameValidator implements EntityValidator<ProperName> {

  @Override
  public void validate(@Nullable ProperName name) {

    if (name == null || "".equals(name.getName())) {
      throw new IllegalArgumentException("ProperName was blank or null");
    }
    // ensure no duplicate Synonyms
    final Map<String, Synonym> duplicates = new HashMap<>();
    for (Synonym synonym : name.getSynonyms()) {
      String synName = synonym.getName();
      Synonym duplicate = duplicates.get(synName);
      if (duplicate == null) {
        duplicates.put(synName, synonym);
      } else {
        throw new IllegalArgumentException("ProperName contains duplicated Synonyms: " + name);
      }
    }
  }
}
