package self.me.matchday.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.RepresentationModel;
import self.me.matchday.model.Team;

public class TeamResource extends RepresentationModel<TeamResource> {

  private final Team team;

  @JsonCreator
  public TeamResource(@JsonProperty("team") final Team team) {
    this.team = team;
  }

  public Team getTeam() {
    return team;
  }
}
