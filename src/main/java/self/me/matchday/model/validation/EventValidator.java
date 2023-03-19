package self.me.matchday.model.validation;

import java.time.LocalDateTime;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import self.me.matchday.api.service.InvalidEventException;
import self.me.matchday.model.Event;
import self.me.matchday.model.Match;

@Component
public class EventValidator implements EntityValidator<Event> {

  private final CompetitionValidator competitionValidator;
  private final TeamValidator teamValidator;
  private final VideoFileSourceValidator fileSourceValidator;

  public EventValidator(
      CompetitionValidator competitionValidator,
      TeamValidator teamValidator,
      VideoFileSourceValidator fileSourceValidator) {
    this.competitionValidator = competitionValidator;
    this.teamValidator = teamValidator;
    this.fileSourceValidator = fileSourceValidator;
  }

  @Override
  public void validate(@Nullable Event event) {

    if (event == null) {
      throw new InvalidEventException("Event is null");
    }
    fileSourceValidator.validateAll(event.getFileSources());
    competitionValidator.validate(event.getCompetition());
    if (event instanceof final Match match) {
      teamValidator.validate(match.getHomeTeam());
      teamValidator.validate(match.getAwayTeam());
    }
    // ensure date is set
    if (event.getDate() == null) {
      event.setDate(LocalDateTime.now());
    }
  }
}
