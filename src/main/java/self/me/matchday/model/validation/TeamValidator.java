package self.me.matchday.model.validation;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import self.me.matchday.model.ProperName;
import self.me.matchday.model.Team;

@Component
public class TeamValidator implements EntityValidator<Team> {

  private final ProperNameValidator nameValidator;

  public TeamValidator(ProperNameValidator nameValidator) {
    this.nameValidator = nameValidator;
  }

  @Override
  public void validate(@Nullable Team team) {

    if (team == null) {
      throw new IllegalArgumentException("Team is null");
    }
    final ProperName name = team.getName();
    nameValidator.validate(name);
  }

  @Override
  public void validateForUpdate(@NotNull Team existing, @NotNull Team updated) {
    validate(updated);
    validateUpdateId(updated);
    validateUpdatedName(existing, updated);
  }

  private void validateUpdateId(@NotNull Team updated) {
    if (updated.getId() == null) {
      throw new IllegalArgumentException("Trying to update unknown Team: " + updated);
    }
  }

  private void validateUpdatedName(@NotNull Team existing, @NotNull Team updated) {

    final ProperName name = updated.getName();
    final String updatedName = name.getName();
    final UUID existingId = existing.getId();
    if (!existingId.equals(updated.getId())) {
      final String msg =
          String.format(
              "A Team with name: %s already exists; please use the merge function instead",
              updatedName);
      throw new IllegalArgumentException(msg);
    }
  }
}
