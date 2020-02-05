package self.me.matchday.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.RepresentationModel;
import self.me.matchday.model.Match;

/**
 * Represents the API view of a Match. Attaches and hides related data as necessary.
 */
public class MatchResource extends RepresentationModel<MatchResource> {

  private final Match match;

  @JsonCreator
  public MatchResource(@JsonProperty("match") final Match match) {
    this.match = match;
  }

  public Match getMatch() {
    return match;
  }
}
