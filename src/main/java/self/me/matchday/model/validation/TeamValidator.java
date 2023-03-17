package self.me.matchday.model.validation;

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
}
