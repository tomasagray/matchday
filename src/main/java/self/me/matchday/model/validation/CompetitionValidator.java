package self.me.matchday.model.validation;

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

  @Override
  public void validate(@Nullable Competition competition) {

    if (competition == null) {
      throw new IllegalArgumentException("Competition is null");
    }
    final ProperName name = competition.getName();
    nameValidator.validate(name);
  }
}
