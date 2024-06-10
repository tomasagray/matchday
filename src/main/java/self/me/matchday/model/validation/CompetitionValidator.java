package self.me.matchday.model.validation;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Competition;
import self.me.matchday.model.ProperName;

@Component
public class CompetitionValidator implements EntityValidator<Competition> {

  private final ProperNameValidator nameValidator;

  public CompetitionValidator(ProperNameValidator nameValidator) {
    this.nameValidator = nameValidator;
  }

  private static void validateId(@NotNull Competition updated) {
    if (updated.getId() == null) {
      throw new IllegalArgumentException("Trying to update unknown Competition: " + updated);
    }
  }

  @Override
  public void validate(@Nullable Competition competition) {
    if (competition == null) {
      throw new IllegalArgumentException("Competition is null");
    }
    final ProperName name = competition.getName();
    nameValidator.validate(name);
  }

  @Override
  public void validateForUpdate(@NotNull Competition existing, @NotNull Competition updated) {
    validate(updated);
    validateId(updated);
    validateUpdatedName(existing, updated);
  }

  private void validateUpdatedName(@NotNull Competition existing, @NotNull Competition updated) {
    final ProperName name = updated.getName();
    final String updatedName = name.getName();
    final UUID existingId = existing.getId();
    if (!existingId.equals(updated.getId())) {
      final String msg =
          String.format(
              "A Competition with name: %s already exists; please use the merge function instead",
              updatedName);
      throw new IllegalArgumentException(msg);
    }
  }
}
