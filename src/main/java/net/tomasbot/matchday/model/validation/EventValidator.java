package net.tomasbot.matchday.model.validation;

import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import net.tomasbot.matchday.api.service.InvalidEventException;
import net.tomasbot.matchday.model.Event;
import net.tomasbot.matchday.model.Match;

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

  @Override
  public void validateForUpdate(@NotNull Event existing, @NotNull Event updated) {
    validate(updated);
    // that's all for now...
  }
}
