package self.me.matchday.model.validation;

import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.SynonymService;
import self.me.matchday.model.Md5Id;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Synonym;

@Component
public class ProperNameValidator implements EntityValidator<ProperName> {

  private final SynonymService synonymService;

  public ProperNameValidator(SynonymService synonymService) {
    this.synonymService = synonymService;
  }

  private static void validateNoDuplicateSynonyms(@NotNull ProperName name) {
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

  @Override
  public void validate(@Nullable ProperName name) {
    validateProperName(name);
    validateProperNameAssociation(name);
    validateNoProperNameSynonymCollision(name);
    validateNoDuplicateSynonyms(name);
  }

  @Override
  public void validateForUpdate(@NotNull ProperName existing, @NotNull ProperName updated) {
    // no checks yet...
  }

  private void validateNoProperNameSynonymCollision(@NotNull ProperName properName) {
    String name = properName.getName();
    Set<Synonym> synonyms = properName.getSynonyms();
    for (Synonym synonym : synonyms) {
      if (name.equals(synonym.getName())) {
        throw new IllegalArgumentException("ProperName <-> Synonym collision: " + name);
      }
    }
  }

  private void validateProperName(ProperName name) {
    if (name == null || name.getName() == null || name.getName().isEmpty()) {
      throw new IllegalArgumentException("ProperName was blank or null");
    }
    final Set<Synonym> synonyms = name.getSynonyms();
    final List<Md5Id> synonymIds = getSynonymIds(synonyms);
    synonyms.forEach(
        synonym -> {
          validateSynonym(synonym, synonymIds);
          validateSynonymIsNotProperName(synonym);
        });
  }

  private void validateProperNameAssociation(@NotNull ProperName properName) {
    final String name = properName.getName();
    synonymService
        .fetchProperName(name)
        .ifPresent(
            existing -> {
              boolean sameId = existing.getId().equals(properName.getId());
              if (!sameId) {
                final String msg =
                    String.format(
                        "The name \"%s\" is already associated with another entity", name);
                throw new IllegalArgumentException(msg);
              }
            });
  }

  @NotNull
  private List<Md5Id> getSynonymIds(@NotNull Set<Synonym> synonyms) {
    return synonyms.stream().map(Synonym::getId).collect(Collectors.toList());
  }

  private void validateSynonymIsNotProperName(@NotNull Synonym synonym) {
    final String name = synonym.getName();
    final Optional<ProperName> nameOptional = synonymService.fetchProperName(name);
    if (nameOptional.isPresent()) {
      ProperName properName = nameOptional.get();
      final String msg =
          String.format("Synonym [%s] already exists as ProperName: %s", name, properName);
      throw new IllegalArgumentException(msg);
    }
  }

  private void validateSynonym(Synonym synonym, List<Md5Id> synonymIds) {
    if (synonym == null) {
      throw new IllegalArgumentException("Synonym was null");
    }
    final String name = synonym.getName();
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Found empty Synonym");
    }
    // check if synonym already exists
    final Optional<Synonym> synonymOptional = synonymService.fetchByName(name);
    if (synonymOptional.isPresent()) {
      final Synonym existingSynonym = synonymOptional.get();
      // synonym exists, but associated with another entity?
      if (!synonymIds.contains(existingSynonym.getId())) {
        final String msg =
            String.format("Synonym: %s already exists and is associated with another entity", name);
        throw new IllegalArgumentException(msg);
      }
    }
  }
}
